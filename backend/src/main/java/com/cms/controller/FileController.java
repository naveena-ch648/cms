package com.cms.controller;

import com.cms.annotation.Audited;
import com.cms.dto.ApiResponse;
import com.cms.dto.file.FileDto;
import com.cms.entity.AuditCategory;
import com.cms.entity.AuditEventType;
import com.cms.dto.file.StorageQuotaDto;
import com.cms.dto.search.SearchRequest;
import com.cms.dto.search.SearchResponse;
import com.cms.entity.FileEntity;
import com.cms.entity.FileEntity.FileStatus;
import com.cms.entity.Folder;
import com.cms.entity.User;
import com.cms.repository.FolderRepository;
import com.cms.repository.UserRepository;
import com.cms.security.UserPrincipal;
import com.cms.service.FileService;
import com.cms.service.RecentFileService;
import com.cms.service.SearchService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;
    private final FolderRepository folderRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final StorageQuotaService storageQuotaService;
    private final SearchService searchService;
    private final RecentFileService recentFileService;

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
    public ResponseEntity<ApiResponse<FileDto>> getFile(
            @PathVariable String fileId,
            @AuthenticationPrincipal UserPrincipal principal) {
        FileEntity file = fileService.getByUuid(fileId);
        // Record this access in the user's personal recent-files history
        recentFileService.recordAccess(principal.getId(), file.getId());
        return ResponseEntity.ok(ApiResponse.ok(FileDto.from(file)));
    }

    /** Lightweight endpoint to track a file view without fetching the full response. */
    @PostMapping("/{fileId}/view")
    public ResponseEntity<ApiResponse<Void>> recordView(
            @PathVariable String fileId,
            @AuthenticationPrincipal UserPrincipal principal) {
        FileEntity file = fileService.getByUuid(fileId);
        recentFileService.recordAccess(principal.getId(), file.getId());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/{fileId}/download")
    @Audited(event = AuditEventType.FILE_DOWNLOADED, category = AuditCategory.FILE_OPERATION, resourceType = "file")
    public ResponseEntity<Void> downloadFile(@PathVariable String fileId) {
        FileEntity file = fileService.getByUuid(fileId);
        fileService.incrementDownloadCount(fileId);

        String url = storageService.presignGetUrl(file.getStorageBucket(), file.getStorageKey(), Duration.ofHours(1));
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(url)).build();
    }

    @PatchMapping("/{fileId}")
    public ResponseEntity<ApiResponse<FileDto>> updateFile(
            @PathVariable String fileId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal principal) {

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new IllegalStateException("User not found"));
        String name = body.containsKey("name") ? (String) body.get("name") : null;
        String description = body.containsKey("description") ? (String) body.get("description") : null;
        String tags = body.containsKey("tags") ? body.get("tags").toString() : null;

        FileEntity updated = fileService.updateFile(fileId, name, description, tags, user);
        return ResponseEntity.ok(ApiResponse.ok(FileDto.from(updated)));
    }

    @PostMapping("/{fileId}/move")
    @Audited(event = AuditEventType.FILE_MOVED, category = AuditCategory.FILE_OPERATION, resourceType = "file")
    public ResponseEntity<ApiResponse<FileDto>> moveFile(
            @PathVariable String fileId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new IllegalStateException("User not found"));
        String targetFolderId = body.get("targetFolderId");
        String onDuplicate = body.getOrDefault("onDuplicate", "rename");

        FileEntity moved = fileService.moveFile(fileId, targetFolderId, onDuplicate, user);
        return ResponseEntity.ok(ApiResponse.ok(FileDto.from(moved)));
    }

    @PostMapping("/{fileId}/copy")
    public ResponseEntity<ApiResponse<FileDto>> copyFile(
            @PathVariable String fileId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new IllegalStateException("User not found"));
        String targetFolderId = body.get("targetFolderId");
        String onDuplicate = body.getOrDefault("onDuplicate", "rename");

        FileEntity copied = fileService.copyFile(fileId, targetFolderId, onDuplicate, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(FileDto.from(copied)));
    }

    @DeleteMapping("/{fileId}")
    @Audited(event = AuditEventType.FILE_DELETED, category = AuditCategory.FILE_OPERATION, resourceType = "file")
    public ResponseEntity<ApiResponse<FileDto>> trashFile(
            @PathVariable String fileId,
            @AuthenticationPrincipal UserPrincipal principal) {

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new IllegalStateException("User not found"));
        FileEntity trashed = fileService.trashFile(fileId, user);
        return ResponseEntity.ok(ApiResponse.ok(FileDto.from(trashed)));
    }

    @PostMapping("/{fileId}/restore")
    public ResponseEntity<ApiResponse<FileDto>> restoreFile(@PathVariable String fileId) {
        FileEntity restored = fileService.restoreFile(fileId);
        return ResponseEntity.ok(ApiResponse.ok(FileDto.from(restored)));
    }

    @DeleteMapping("/{fileId}/permanent")
    @Audited(event = AuditEventType.FILE_DELETED, category = AuditCategory.FILE_OPERATION, resourceType = "file")
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
    public ResponseEntity<ApiResponse<StorageQuotaDto>> getQuota(@AuthenticationPrincipal UserPrincipal principal) {
        var quota = storageQuotaService.getQuotaForOrg(principal.getOrganizationId());
        return ResponseEntity.ok(ApiResponse.ok(StorageQuotaDto.from(quota)));
    }

    @GetMapping("/filter")
    public ResponseEntity<ApiResponse<SearchResponse>> filterFiles(
            @RequestParam String workspaceId,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(required = false) List<String> fileType,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "relevance") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder,
            @RequestParam Map<String, String> allParams,
            @AuthenticationPrincipal UserPrincipal principal) {

        Map<String, String> metadataFilters = new HashMap<>();
        for (Map.Entry<String, String> entry : allParams.entrySet()) {
            if (entry.getKey().startsWith("meta.")) {
                metadataFilters.put(entry.getKey().substring(5), entry.getValue());
            }
        }

        SearchRequest searchRequest = SearchRequest.builder()
                .workspaceId(workspaceId)
                .query(query)
                .tags(tags)
                .metadataFilters(metadataFilters.isEmpty() ? null : metadataFilters)
                .fileType(fileType)
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .page(page)
                .size(Math.min(size, 100))
                .sortBy(sortBy)
                .sortOrder(sortOrder)
                .build();

        SearchResponse response = searchService.search(searchRequest);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
