package com.cms.service;

import com.cms.dto.file.ChunkUploadResponse;
import com.cms.dto.file.UploadInitiateRequest;
import com.cms.dto.file.UploadInitiateResponse;
import com.cms.dto.file.UploadSessionStatusDto;
import com.cms.entity.FileEntity;
import com.cms.entity.Folder;
import com.cms.entity.User;
import com.cms.repository.FolderRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
@Slf4j
public class FileUploadService {

    private final StorageService storageService;
    private final StorageQuotaService storageQuotaService;
    private final FileService fileService;
    private final FolderRepository folderRepository;
    private final JdbcTemplate pgJdbc;
    private final ObjectMapper objectMapper;

    @Value("${file-upload.default-chunk-size:5242880}")
    private long defaultChunkSize;

    @Value("${file-upload.session-ttl-hours:24}")
    private int sessionTtlHours;

    public FileUploadService(StorageService storageService,
                             StorageQuotaService storageQuotaService,
                             FileService fileService,
                             FolderRepository folderRepository,
                             @Qualifier("pgJdbcTemplate") JdbcTemplate pgJdbc,
                             ObjectMapper objectMapper) {
        this.storageService = storageService;
        this.storageQuotaService = storageQuotaService;
        this.fileService = fileService;
        this.folderRepository = folderRepository;
        this.pgJdbc = pgJdbc;
        this.objectMapper = objectMapper;
    }

    public FileEntity uploadSingleFile(MultipartFile file, String folderUuid, String description,
                                       String tags, String onDuplicate, User uploader) throws IOException {
        Folder folder = folderRepository.findByUuid(folderUuid)
                .orElseThrow(() -> new IllegalArgumentException("Folder not found: " + folderUuid));

        Long orgId = folder.getWorkspace().getOrganization().getId();

        storageQuotaService.validateFileSize(orgId, file.getSize());
        storageQuotaService.validateFileExtension(orgId, file.getOriginalFilename());
        if (!storageQuotaService.checkQuotaAvailable(orgId, file.getSize())) {
            throw new IllegalStateException("Storage quota exceeded");
        }

        String bucket = "cms-" + folder.getWorkspace().getOrganization().getId();
        String fileUuid = UUID.randomUUID().toString();
        String storageKey = folder.getWorkspace().getId() + "/" + folder.getUuid() + "/" + fileUuid + "_" + file.getOriginalFilename();

        storageService.putObject(bucket, storageKey, file.getInputStream(), file.getSize(), file.getContentType());

        return fileService.createFileRecord(folder, uploader, file.getOriginalFilename(),
                file.getOriginalFilename(), file.getSize(), file.getContentType(),
                storageKey, bucket, description, tags, onDuplicate);
    }

    public UploadInitiateResponse initiateChunkedUpload(UploadInitiateRequest request, User uploader) {
        Folder folder = folderRepository.findByUuid(request.getFolderId())
                .orElseThrow(() -> new IllegalArgumentException("Folder not found: " + request.getFolderId()));

        Long orgId = folder.getWorkspace().getOrganization().getId();

        storageQuotaService.validateFileSize(orgId, request.getFileSize());
        storageQuotaService.validateFileExtension(orgId, request.getFileName());
        if (!storageQuotaService.checkQuotaAvailable(orgId, request.getFileSize())) {
            throw new IllegalStateException("Storage quota exceeded");
        }

        long chunkSize = request.getChunkSize() != null ? request.getChunkSize() : defaultChunkSize;
        int totalChunks = (int) Math.ceil((double) request.getFileSize() / chunkSize);

        String bucket = "cms-" + orgId;
        String fileUuid = UUID.randomUUID().toString();
        String storageKey = folder.getWorkspace().getId() + "/" + folder.getUuid() + "/" + fileUuid + "_" + request.getFileName();

        String sessionId = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(Duration.ofHours(sessionTtlHours));

        pgJdbc.update("""
                INSERT INTO upload_sessions (session_id, file_name, folder_id, folder_uuid,
                    organization_id, workspace_id, uploaded_by, total_size, chunk_size, total_chunks,
                    mime_type, bucket, storage_key, description, tags, on_duplicate, expires_at)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """,
                sessionId, request.getFileName(), folder.getId(), folder.getUuid(),
                orgId, folder.getWorkspace().getId(), uploader.getId(),
                request.getFileSize(), chunkSize, totalChunks,
                request.getMimeType(), bucket, storageKey,
                request.getDescription() != null ? request.getDescription() : "",
                request.getTags() != null ? request.getTags() : "[]",
                request.getOnDuplicate() != null ? request.getOnDuplicate() : "rename",
                java.sql.Timestamp.from(expiresAt));

        return UploadInitiateResponse.builder()
                .sessionId(sessionId)
                .chunkSize(chunkSize)
                .totalChunks(totalChunks)
                .expiresAt(expiresAt)
                .build();
    }

    public ChunkUploadResponse uploadChunk(String sessionId, int chunkNumber, byte[] data) throws IOException {
        Map<String, Object> session = getSession(sessionId);
        int totalChunks = ((Number) session.get("total_chunks")).intValue();

        if (chunkNumber < 0 || chunkNumber >= totalChunks) {
            throw new IllegalArgumentException("Invalid chunk number: " + chunkNumber);
        }

        String etag = Integer.toHexString(Arrays.hashCode(data));

        pgJdbc.update("""
                INSERT INTO upload_session_parts (session_id, chunk_number, etag, data)
                VALUES (?,?,?,?)
                ON CONFLICT (session_id, chunk_number) DO UPDATE SET etag=EXCLUDED.etag, data=EXCLUDED.data
                """, sessionId, chunkNumber, etag, data);

        List<Integer> completed = getCompletedChunks(sessionId, totalChunks);

        pgJdbc.update("""
                UPDATE upload_sessions SET status='IN_PROGRESS',
                    completed_chunks=?, last_activity_at=NOW() WHERE session_id=?
                """, toJson(completed), sessionId);

        return ChunkUploadResponse.builder()
                .chunkNumber(chunkNumber)
                .received(true)
                .completedChunks(completed.size())
                .totalChunks(totalChunks)
                .build();
    }

    public FileEntity completeChunkedUpload(String sessionId, String checksumSha256, User uploader) {
        Map<String, Object> session = getSession(sessionId);
        int totalChunks = ((Number) session.get("total_chunks")).intValue();
        List<Integer> completed = getCompletedChunks(sessionId, totalChunks);

        if (completed.size() != totalChunks) {
            throw new IllegalStateException("Not all chunks uploaded. Expected " + totalChunks + ", got " + completed.size());
        }

        String bucket = (String) session.get("bucket");
        String storageKey = (String) session.get("storage_key");
        String mimeType = (String) session.get("mime_type");
        long totalSize = ((Number) session.get("total_size")).longValue();

        // Assemble all chunk bytes in order
        List<byte[]> parts = new ArrayList<>();
        for (int i = 0; i < totalChunks; i++) {
            byte[] chunk = pgJdbc.queryForObject(
                    "SELECT data FROM upload_session_parts WHERE session_id=? AND chunk_number=?",
                    byte[].class, sessionId, i);
            if (chunk == null) throw new IllegalStateException("Missing chunk " + i);
            parts.add(chunk);
        }

        // Concatenate and store
        byte[] assembled = assembleChunks(parts);
        storageService.putObject(bucket, storageKey,
                new java.io.ByteArrayInputStream(assembled), assembled.length, mimeType);

        Folder folder = folderRepository.findById(((Number) session.get("folder_id")).longValue()).orElseThrow();

        String description = (String) session.get("description");
        FileEntity file = fileService.createFileRecord(
                folder, uploader,
                (String) session.get("file_name"),
                (String) session.get("file_name"),
                totalSize, mimeType, storageKey, bucket,
                (description == null || description.isEmpty()) ? null : description,
                (String) session.get("tags"),
                (String) session.get("on_duplicate"));

        if (checksumSha256 != null && !checksumSha256.isBlank()) {
            file.setChecksumSha256(checksumSha256);
        }

        cleanupSession(sessionId);
        return file;
    }

    public void abortUpload(String sessionId) {
        cleanupSession(sessionId);
    }

    public UploadSessionStatusDto getSessionStatus(String sessionId) {
        Map<String, Object> session = getSession(sessionId);
        int totalChunks = ((Number) session.get("total_chunks")).intValue();
        List<Integer> completed = getCompletedChunks(sessionId, totalChunks);
        double percent = totalChunks > 0 ? (double) completed.size() / totalChunks * 100.0 : 0;

        Instant createdAt = ((java.sql.Timestamp) session.get("created_at")).toInstant();
        Instant lastActivity = ((java.sql.Timestamp) session.get("last_activity_at")).toInstant();

        return UploadSessionStatusDto.builder()
                .sessionId(sessionId)
                .fileName((String) session.get("file_name"))
                .totalChunks(totalChunks)
                .completedChunks(completed.size())
                .percentComplete(Math.round(percent * 10.0) / 10.0)
                .status((String) session.get("status"))
                .expiresAt(createdAt.plus(Duration.ofHours(sessionTtlHours)))
                .lastActivityAt(lastActivity)
                .build();
    }

    // ──────── helpers ────────

    private Map<String, Object> getSession(String sessionId) {
        List<Map<String, Object>> rows = pgJdbc.queryForList(
                "SELECT * FROM upload_sessions WHERE session_id=? AND expires_at > NOW()", sessionId);
        if (rows.isEmpty()) throw new IllegalArgumentException("Upload session not found or expired: " + sessionId);
        return rows.get(0);
    }

    private List<Integer> getCompletedChunks(String sessionId, int totalChunks) {
        return pgJdbc.queryForList(
                "SELECT chunk_number FROM upload_session_parts WHERE session_id=? ORDER BY chunk_number",
                Integer.class, sessionId);
    }

    private void cleanupSession(String sessionId) {
        pgJdbc.update("DELETE FROM upload_session_parts WHERE session_id=?", sessionId);
        pgJdbc.update("DELETE FROM upload_sessions WHERE session_id=?", sessionId);
    }

    private byte[] assembleChunks(List<byte[]> parts) {
        int total = parts.stream().mapToInt(b -> b.length).sum();
        byte[] result = new byte[total];
        int offset = 0;
        for (byte[] part : parts) {
            System.arraycopy(part, 0, result, offset, part.length);
            offset += part.length;
        }
        return result;
    }

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); } catch (Exception e) { return "[]"; }
    }
}
