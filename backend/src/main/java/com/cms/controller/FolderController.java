package com.cms.controller;

import com.cms.dto.ApiResponse;
import com.cms.dto.folder.*;
import com.cms.entity.Folder;
import com.cms.entity.FolderPermission;
import com.cms.security.UserPrincipal;
import com.cms.service.FolderPermissionService;
import com.cms.service.FolderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/folders")
@RequiredArgsConstructor
public class FolderController {

    private final FolderService folderService;
    private final FolderPermissionService folderPermissionService;

    @PostMapping
    @PreAuthorize("hasPermission(null, 'manage-folders')")
    public ResponseEntity<ApiResponse<FolderResponse>> create(
            @PathVariable String workspaceId,
            @Valid @RequestBody CreateFolderRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        Folder folder = folderService.create(
                workspaceId, request.getName(), request.getParentId(),
                request.getSortOrder(), principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(FolderResponse.from(folder)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FolderTreeResponse>>> list(
            @PathVariable String workspaceId,
            @RequestParam(defaultValue = "false") boolean lazy) {
        List<FolderTreeResponse> folders = folderService.listByWorkspace(workspaceId, lazy);
        return ResponseEntity.ok(ApiResponse.ok(folders));
    }

    @GetMapping("/{folderId}")
    public ResponseEntity<ApiResponse<FolderResponse>> getById(
            @PathVariable String workspaceId,
            @PathVariable String folderId) {
        FolderResponse response = folderService.getByUuidWithBreadcrumbs(folderId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{folderId}/children")
    public ResponseEntity<ApiResponse<List<FolderTreeResponse>>> getChildren(
            @PathVariable String workspaceId,
            @PathVariable String folderId) {
        List<FolderTreeResponse> children = folderService.getChildren(folderId);
        return ResponseEntity.ok(ApiResponse.ok(children));
    }

    @PutMapping("/{folderId}")
    @PreAuthorize("hasPermission(null, 'manage-folders')")
    public ResponseEntity<ApiResponse<FolderResponse>> update(
            @PathVariable String workspaceId,
            @PathVariable String folderId,
            @Valid @RequestBody UpdateFolderRequest request) {
        Folder folder = folderService.update(folderId, request.getName(), request.getSortOrder());
        return ResponseEntity.ok(ApiResponse.ok(FolderResponse.from(folder)));
    }

    @DeleteMapping("/{folderId}")
    @PreAuthorize("hasPermission(null, 'manage-folders')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable String workspaceId,
            @PathVariable String folderId) {
        folderService.delete(folderId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PutMapping("/{folderId}/move")
    @PreAuthorize("hasPermission(null, 'manage-folders')")
    public ResponseEntity<ApiResponse<FolderResponse>> move(
            @PathVariable String workspaceId,
            @PathVariable String folderId,
            @Valid @RequestBody MoveFolderRequest request) {
        Folder folder = folderService.move(folderId, request.getTargetParentId(), request.getSortOrder());
        return ResponseEntity.ok(ApiResponse.ok(FolderResponse.from(folder)));
    }

    // --- Permission endpoints ---

    @GetMapping("/{folderId}/permissions")
    public ResponseEntity<ApiResponse<List<FolderPermissionResponse>>> listPermissions(
            @PathVariable String workspaceId,
            @PathVariable String folderId) {
        List<FolderPermissionResponse> permissions = folderPermissionService.listPermissions(folderId);
        return ResponseEntity.ok(ApiResponse.ok(permissions));
    }

    @PostMapping("/{folderId}/permissions")
    @PreAuthorize("hasPermission(null, 'manage-folders')")
    public ResponseEntity<ApiResponse<FolderPermissionResponse>> assignPermission(
            @PathVariable String workspaceId,
            @PathVariable String folderId,
            @Valid @RequestBody FolderPermissionRequest request) {
        FolderPermission fp = folderPermissionService.assignPermission(
                folderId, request.getUserId(), request.getGroupId(), request.getRoleId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(FolderPermissionResponse.from(fp)));
    }

    @DeleteMapping("/{folderId}/permissions/{permissionId}")
    @PreAuthorize("hasPermission(null, 'manage-folders')")
    public ResponseEntity<ApiResponse<Void>> removePermission(
            @PathVariable String workspaceId,
            @PathVariable String folderId,
            @PathVariable Long permissionId) {
        folderPermissionService.removePermission(permissionId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // --- Favorite endpoints ---

    @PostMapping("/{folderId}/favorite")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> addFavorite(
            @PathVariable String workspaceId,
            @PathVariable String folderId,
            @AuthenticationPrincipal UserPrincipal principal) {
        folderService.addFavorite(folderId, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(java.util.Map.of(
                        "folderId", folderId,
                        "favoritedAt", java.time.Instant.now().toString()
                )));
    }

    @DeleteMapping("/{folderId}/favorite")
    public ResponseEntity<ApiResponse<Void>> removeFavorite(
            @PathVariable String workspaceId,
            @PathVariable String folderId,
            @AuthenticationPrincipal UserPrincipal principal) {
        folderService.removeFavorite(folderId, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/{folderId}/visit")
    public ResponseEntity<ApiResponse<Void>> recordVisit(
            @PathVariable String workspaceId,
            @PathVariable String folderId,
            @AuthenticationPrincipal UserPrincipal principal) {
        folderService.recordVisit(folderId, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
