package com.cms.controller;

import com.cms.dto.ApiResponse;
import com.cms.dto.file.FileDto;
import com.cms.dto.file.StorageQuotaDto;
import com.cms.entity.FileEntity;
import com.cms.entity.FileEntity.FileStatus;
import com.cms.entity.Folder;
import com.cms.entity.User;
import com.cms.repository.FolderRepository;
import com.cms.service.FileService;
import com.cms.service.StorageQuotaService;
import com.cms.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;
    private final FolderRepository folderRepository;
    private final StorageService storageService;
    private final StorageQuotaService storageQuotaService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<FileDto>>> listFiles(
            @RequestParam String folderId,
            @RequestParam(defaultValue = "ACTIVE") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sort,
            @RequestParam(defaultValue = "asc") String direction) {

        Folder folder = folderRepository.findByUuid(folderId)
                .orElseThrow(() -> new IllegalArgumentException("Folder not found: " + folderId));

        Sort sortObj = direction.equalsIgnoreCase("desc")
                ? Sort.by(sort).descending()
                : Sort.by(sort).ascending();
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), sortObj);

        FileStatus fileStatus = FileStatus.valueOf(status.toUpperCase());
        Page<FileDto> files = fileService.listByFolder(folder.getId(), fileStatus, pageable)
                .map(FileDto::from);

        return ResponseEntity.ok(ApiResponse.ok(files));
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<ApiResponse<FileDto>> getFile(@PathVariable String fileId) {
        FileEntity file = fileService.getByUuid(fileId);
        return ResponseEntity.ok(ApiResponse.ok(FileDto.from(file)));
    }

    @GetMapping("/{fileId}/download")
    public ResponseEntity<Void> downloadFile(@PathVariable String fileId) {
        FileEntity file = fileService.getByUuid(fileId);
        fileService.incrementDownloadCount(fileId);

        String url = storageService.presignGetUrl(file.getStorageBucket(), file.getStorageKey(), Duration.ofHours(1));
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(url)).build();
    }

    @GetMapping("/{fileId}/preview")
    public ResponseEntity<ApiResponse<Map<String, Object>>> previewFile(@PathVariable String fileId) {
        FileEntity file = fileService.getByUuid(fileId);
        String mimeType = file.getMimeType();

        if (!isPreviewable(mimeType)) {
            throw new IllegalArgumentException("File type not previewable: " + mimeType);
        }

        String url = storageService.presignGetUrl(file.getStorageBucket(), file.getStorageKey(), Duration.ofHours(1));
        Instant expiresAt = Instant.now().plus(Duration.ofHours(1));

        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "previewUrl", url,
                "mimeType", mimeType,
                "expiresAt", expiresAt.toString()
        )));
    }

    @PatchMapping("/{fileId}")
    public ResponseEntity<ApiResponse<FileDto>> updateFile(
            @PathVariable String fileId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal User user) {

        String name = body.containsKey("name") ? (String) body.get("name") : null;
        String description = body.containsKey("description") ? (String) body.get("description") : null;
        String tags = body.containsKey("tags") ? body.get("tags").toString() : null;

        FileEntity updated = fileService.updateFile(fileId, name, description, tags, user);
        return ResponseEntity.ok(ApiResponse.ok(FileDto.from(updated)));
    }

    @PostMapping("/{fileId}/move")
    public ResponseEntity<ApiResponse<FileDto>> moveFile(
            @PathVariable String fileId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User user) {

        String targetFolderId = body.get("targetFolderId");
        String onDuplicate = body.getOrDefault("onDuplicate", "rename");

        FileEntity moved = fileService.moveFile(fileId, targetFolderId, onDuplicate, user);
        return ResponseEntity.ok(ApiResponse.ok(FileDto.from(moved)));
    }

    @PostMapping("/{fileId}/copy")
    public ResponseEntity<ApiResponse<FileDto>> copyFile(
            @PathVariable String fileId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User user) {

        String targetFolderId = body.get("targetFolderId");
        String onDuplicate = body.getOrDefault("onDuplicate", "rename");

        FileEntity copied = fileService.copyFile(fileId, targetFolderId, onDuplicate, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(FileDto.from(copied)));
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<ApiResponse<FileDto>> trashFile(
            @PathVariable String fileId,
            @AuthenticationPrincipal User user) {

        FileEntity trashed = fileService.trashFile(fileId, user);
        return ResponseEntity.ok(ApiResponse.ok(FileDto.from(trashed)));
    }

    @PostMapping("/{fileId}/restore")
    public ResponseEntity<ApiResponse<FileDto>> restoreFile(@PathVariable String fileId) {
        FileEntity restored = fileService.restoreFile(fileId);
        return ResponseEntity.ok(ApiResponse.ok(FileDto.from(restored)));
    }

    @DeleteMapping("/{fileId}/permanent")
    public ResponseEntity<Void> permanentDelete(@PathVariable String fileId) {
        fileService.permanentDelete(fileId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/trash")
    public ResponseEntity<ApiResponse<Page<FileDto>>> listTrash(
            @RequestParam String workspaceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // Find workspace by uuid to get internal id
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("trashedAt").descending());
        // For now we query by workspace using a repository method
        // This is simplified - would need workspace lookup
        Page<FileDto> trash = fileService.listTrash(null, pageable).map(FileDto::from);
        return ResponseEntity.ok(ApiResponse.ok(trash));
    }

    @GetMapping("/quota")
    public ResponseEntity<ApiResponse<StorageQuotaDto>> getQuota(@AuthenticationPrincipal User user) {
        // Get org from user context
        var quota = storageQuotaService.getQuotaForOrg(user.getOrganization().getId());
        return ResponseEntity.ok(ApiResponse.ok(StorageQuotaDto.from(quota)));
    }

    private boolean isPreviewable(String mimeType) {
        if (mimeType == null) return false;
        return mimeType.startsWith("image/") ||
               mimeType.equals("application/pdf") ||
               mimeType.startsWith("text/") ||
               mimeType.startsWith("video/") ||
               mimeType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") ||
               mimeType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") ||
               mimeType.equals("application/vnd.openxmlformats-officedocument.presentationml.presentation") ||
               mimeType.equals("application/msword") ||
               mimeType.equals("application/vnd.ms-excel") ||
               mimeType.equals("application/vnd.ms-powerpoint");
    }
}
