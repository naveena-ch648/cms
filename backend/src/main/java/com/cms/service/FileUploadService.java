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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileUploadService {

    private final StorageService storageService;
    private final StorageQuotaService storageQuotaService;
    private final FileService fileService;
    private final FolderRepository folderRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${file-upload.default-chunk-size:5242880}")
    private long defaultChunkSize;

    @Value("${file-upload.session-ttl-hours:24}")
    private int sessionTtlHours;

    private static final String SESSION_PREFIX = "upload_session:";

    public FileEntity uploadSingleFile(MultipartFile file, String folderUuid, String description,
                                       String tags, String onDuplicate, User uploader) throws IOException {
        Folder folder = folderRepository.findByUuid(folderUuid)
                .orElseThrow(() -> new IllegalArgumentException("Folder not found: " + folderUuid));

        Long orgId = folder.getWorkspace().getOrganization().getId();

        // Validate
        storageQuotaService.validateFileSize(orgId, file.getSize());
        storageQuotaService.validateFileExtension(orgId, file.getOriginalFilename());
        if (!storageQuotaService.checkQuotaAvailable(orgId, file.getSize())) {
            throw new IllegalStateException("Storage quota exceeded");
        }

        // Generate storage key
        String bucket = "cms-" + folder.getWorkspace().getOrganization().getId();
        String fileUuid = UUID.randomUUID().toString();
        String storageKey = folder.getWorkspace().getId() + "/" + folder.getUuid() + "/" + fileUuid + "_" + file.getOriginalFilename();

        // Upload to MinIO
        storageService.putObject(bucket, storageKey, file.getInputStream(), file.getSize(), file.getContentType());

        // Create file record
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

        // Initiate S3 multipart upload
        String s3UploadId = storageService.initiateMultipartUpload(bucket, storageKey, request.getMimeType());

        // Store session in Redis
        String sessionId = UUID.randomUUID().toString();
        Map<String, String> session = new HashMap<>();
        session.put("sessionId", sessionId);
        session.put("fileName", request.getFileName());
        session.put("folderId", String.valueOf(folder.getId()));
        session.put("folderUuid", folder.getUuid());
        session.put("organizationId", String.valueOf(orgId));
        session.put("workspaceId", String.valueOf(folder.getWorkspace().getId()));
        session.put("uploadedBy", String.valueOf(uploader.getId()));
        session.put("totalSize", String.valueOf(request.getFileSize()));
        session.put("chunkSize", String.valueOf(chunkSize));
        session.put("totalChunks", String.valueOf(totalChunks));
        session.put("completedChunks", "[]");
        session.put("s3UploadId", s3UploadId);
        session.put("s3Bucket", bucket);
        session.put("s3Key", storageKey);
        session.put("mimeType", request.getMimeType());
        session.put("status", "INITIATED");
        session.put("description", request.getDescription() != null ? request.getDescription() : "");
        session.put("tags", request.getTags() != null ? request.getTags() : "[]");
        session.put("onDuplicate", request.getOnDuplicate() != null ? request.getOnDuplicate() : "rename");
        session.put("createdAt", Instant.now().toString());
        session.put("lastActivityAt", Instant.now().toString());

        String key = SESSION_PREFIX + sessionId;
        redisTemplate.opsForHash().putAll(key, session);
        redisTemplate.expire(key, sessionTtlHours, TimeUnit.HOURS);

        return UploadInitiateResponse.builder()
                .sessionId(sessionId)
                .chunkSize(chunkSize)
                .totalChunks(totalChunks)
                .expiresAt(Instant.now().plus(Duration.ofHours(sessionTtlHours)))
                .build();
    }

    public ChunkUploadResponse uploadChunk(String sessionId, int chunkNumber,
                                           byte[] data) throws IOException {
        String key = SESSION_PREFIX + sessionId;
        Map<Object, Object> session = redisTemplate.opsForHash().entries(key);
        if (session.isEmpty()) {
            throw new IllegalArgumentException("Upload session not found or expired");
        }

        int totalChunks = Integer.parseInt((String) session.get("totalChunks"));
        if (chunkNumber < 0 || chunkNumber >= totalChunks) {
            throw new IllegalArgumentException("Invalid chunk number: " + chunkNumber);
        }

        String bucket = (String) session.get("s3Bucket");
        String storageKey = (String) session.get("s3Key");
        String uploadId = (String) session.get("s3UploadId");

        // Upload part to S3 (part numbers are 1-based)
        String etag = storageService.uploadPart(bucket, storageKey, uploadId, chunkNumber + 1,
                new java.io.ByteArrayInputStream(data), data.length);

        // Update completed chunks
        List<Integer> completed = parseCompletedChunks((String) session.get("completedChunks"));
        if (!completed.contains(chunkNumber)) {
            completed.add(chunkNumber);
        }

        redisTemplate.opsForHash().put(key, "completedChunks", objectMapper.writeValueAsString(completed));
        redisTemplate.opsForHash().put(key, "status", "IN_PROGRESS");
        redisTemplate.opsForHash().put(key, "lastActivityAt", Instant.now().toString());
        // Store etag for part
        redisTemplate.opsForHash().put(key, "etag_" + chunkNumber, etag);

        return ChunkUploadResponse.builder()
                .chunkNumber(chunkNumber)
                .received(true)
                .completedChunks(completed.size())
                .totalChunks(totalChunks)
                .build();
    }

    public FileEntity completeChunkedUpload(String sessionId, String checksumSha256, User uploader) {
        String key = SESSION_PREFIX + sessionId;
        Map<Object, Object> session = redisTemplate.opsForHash().entries(key);
        if (session.isEmpty()) {
            throw new IllegalArgumentException("Upload session not found or expired");
        }

        int totalChunks = Integer.parseInt((String) session.get("totalChunks"));
        List<Integer> completed = parseCompletedChunks((String) session.get("completedChunks"));

        if (completed.size() != totalChunks) {
            throw new IllegalStateException("Not all chunks uploaded. Expected " + totalChunks + ", got " + completed.size());
        }

        String bucket = (String) session.get("s3Bucket");
        String storageKey = (String) session.get("s3Key");
        String uploadId = (String) session.get("s3UploadId");

        // Build completed parts list
        List<software.amazon.awssdk.services.s3.model.CompletedPart> parts = new ArrayList<>();
        for (int i = 0; i < totalChunks; i++) {
            String etag = (String) redisTemplate.opsForHash().get(key, "etag_" + i);
            parts.add(software.amazon.awssdk.services.s3.model.CompletedPart.builder()
                    .partNumber(i + 1)
                    .eTag(etag)
                    .build());
        }

        // Complete S3 multipart
        storageService.completeMultipartUpload(bucket, storageKey, uploadId, parts);

        // Create file record
        Folder folder = folderRepository.findById(Long.parseLong((String) session.get("folderId")))
                .orElseThrow();

        FileEntity file = fileService.createFileRecord(
                folder, uploader,
                (String) session.get("fileName"),
                (String) session.get("fileName"),
                Long.parseLong((String) session.get("totalSize")),
                (String) session.get("mimeType"),
                storageKey, bucket,
                ((String) session.get("description")).isEmpty() ? null : (String) session.get("description"),
                (String) session.get("tags"),
                (String) session.get("onDuplicate"));

        if (checksumSha256 != null && !checksumSha256.isBlank()) {
            file.setChecksumSha256(checksumSha256);
        }

        // Cleanup Redis session
        redisTemplate.delete(key);

        return file;
    }

    public void abortUpload(String sessionId) {
        String key = SESSION_PREFIX + sessionId;
        Map<Object, Object> session = redisTemplate.opsForHash().entries(key);
        if (session.isEmpty()) {
            return;
        }

        String bucket = (String) session.get("s3Bucket");
        String storageKey = (String) session.get("s3Key");
        String uploadId = (String) session.get("s3UploadId");

        storageService.abortMultipartUpload(bucket, storageKey, uploadId);
        redisTemplate.delete(key);
    }

    public UploadSessionStatusDto getSessionStatus(String sessionId) {
        String key = SESSION_PREFIX + sessionId;
        Map<Object, Object> session = redisTemplate.opsForHash().entries(key);
        if (session.isEmpty()) {
            throw new IllegalArgumentException("Upload session not found or expired");
        }

        int totalChunks = Integer.parseInt((String) session.get("totalChunks"));
        List<Integer> completed = parseCompletedChunks((String) session.get("completedChunks"));
        double percent = totalChunks > 0 ? (double) completed.size() / totalChunks * 100.0 : 0;

        return UploadSessionStatusDto.builder()
                .sessionId(sessionId)
                .fileName((String) session.get("fileName"))
                .totalChunks(totalChunks)
                .completedChunks(completed.size())
                .percentComplete(Math.round(percent * 10.0) / 10.0)
                .status((String) session.get("status"))
                .expiresAt(Instant.parse((String) session.get("createdAt")).plus(Duration.ofHours(sessionTtlHours)))
                .lastActivityAt(Instant.parse((String) session.get("lastActivityAt")))
                .build();
    }

    private List<Integer> parseCompletedChunks(String json) {
        if (json == null || json.isBlank() || "[]".equals(json)) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Integer>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
