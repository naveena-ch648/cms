package com.cms.controller;

import com.cms.dto.ApiResponse;
import com.cms.dto.workspace.*;
import com.cms.entity.FolderFavorite;
import com.cms.entity.FolderRecent;
import com.cms.entity.Role;
import com.cms.entity.UserWorkspaceRole;
import com.cms.entity.Workspace;
import com.cms.middleware.TenantContext;
import com.cms.repository.UserWorkspaceRoleRepository;
import com.cms.security.UserPrincipal;
import com.cms.service.FolderService;
import com.cms.service.WorkspaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;
    private final FolderService folderService;
    private final UserWorkspaceRoleRepository userWorkspaceRoleRepository;

        @PostMapping
        @PreAuthorize("hasPermission(null, 'manage-workspace')")
    public ResponseEntity<ApiResponse<WorkspaceResponse>> create(
            @Valid @RequestBody CreateWorkspaceRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long orgId = TenantContext.getCurrentTenant();
        Workspace workspace = workspaceService.create(
                orgId, request.getName(), request.getDescription(), principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(WorkspaceResponse.from(workspace)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<WorkspaceResponse>>> list(
            Pageable pageable,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long orgId = TenantContext.getCurrentTenant();
        Page<Workspace> page = workspaceService.list(orgId, pageable);
        List<WorkspaceResponse> workspaces = page.getContent().stream()
                .map(ws -> {
                    int count = (int) userWorkspaceRoleRepository.countByWorkspaceId(ws.getId());
                    Role myRole = userWorkspaceRoleRepository
                            .findByUserIdAndWorkspaceId(principal.getId(), ws.getId())
                            .map(UserWorkspaceRole::getRole)
                            .orElse(null);
                    return WorkspaceResponse.from(ws, count, myRole);
                }).toList();
        ApiResponse.PagedMeta meta = ApiResponse.PagedMeta.builder()
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
        return ResponseEntity.ok(ApiResponse.ok(workspaces, meta));
    }

    @GetMapping("/{workspaceId}")
    public ResponseEntity<ApiResponse<WorkspaceResponse>> getById(
            @PathVariable String workspaceId,
            @AuthenticationPrincipal UserPrincipal principal) {
        Workspace workspace = workspaceService.getById(workspaceId);
        int count = (int) userWorkspaceRoleRepository.countByWorkspaceId(workspace.getId());
        Role myRole = userWorkspaceRoleRepository
                .findByUserIdAndWorkspaceId(principal.getId(), workspace.getId())
                .map(UserWorkspaceRole::getRole)
                .orElse(null);
        return ResponseEntity.ok(ApiResponse.ok(WorkspaceResponse.from(workspace, count, myRole)));
    }

        @PutMapping("/{workspaceId}")
        @PreAuthorize("hasPermission(null, 'manage-workspace')")
    public ResponseEntity<ApiResponse<WorkspaceResponse>> update(
            @PathVariable String workspaceId, @Valid @RequestBody UpdateWorkspaceRequest request) {
        Workspace workspace = workspaceService.update(
                workspaceId, request.getName(), request.getDescription());
        return ResponseEntity.ok(ApiResponse.ok(WorkspaceResponse.from(workspace)));
    }

        @PutMapping("/{workspaceId}/archive")
        @PreAuthorize("hasPermission(null, 'manage-workspace')")
    public ResponseEntity<ApiResponse<WorkspaceResponse>> archive(@PathVariable String workspaceId) {
        Workspace workspace = workspaceService.archive(workspaceId);
        return ResponseEntity.ok(ApiResponse.ok(WorkspaceResponse.from(workspace)));
    }

        @DeleteMapping("/{workspaceId}")
        @PreAuthorize("hasPermission(null, 'manage-workspace')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String workspaceId) {
        workspaceService.delete(workspaceId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

        @PostMapping("/{workspaceId}/members")
        @PreAuthorize("hasPermission(null, 'manage-workspace')")
    public ResponseEntity<ApiResponse<Void>> assignMember(
            @PathVariable String workspaceId,
            @Valid @RequestBody AssignWorkspaceRoleRequest request) {
        workspaceService.assignUserRole(workspaceId, request.getUserId(), request.getRoleId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(null));
    }

        @DeleteMapping("/{workspaceId}/members/{userId}")
        @PreAuthorize("hasPermission(null, 'manage-workspace')")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable String workspaceId, @PathVariable Long userId) {
        workspaceService.removeUserRole(workspaceId, userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/{workspaceId}/favorites")
    public ResponseEntity<ApiResponse<List<java.util.Map<String, Object>>>> listFavorites(
            @PathVariable String workspaceId,
            @AuthenticationPrincipal UserPrincipal principal) {
        List<FolderFavorite> favorites = folderService.listFavorites(principal.getId(), workspaceId);
        List<java.util.Map<String, Object>> result = favorites.stream()
                .map(fav -> java.util.Map.<String, Object>of(
                        "id", fav.getFolder().getUuid(),
                        "name", fav.getFolder().getName(),
                        "parentId", fav.getFolder().getParent() != null ? fav.getFolder().getParent().getUuid() : "",
                        "favoritedAt", fav.getCreatedAt().toString()
                ))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{workspaceId}/recents")
    public ResponseEntity<ApiResponse<List<java.util.Map<String, Object>>>> listRecents(
            @PathVariable String workspaceId,
            @AuthenticationPrincipal UserPrincipal principal) {
        List<FolderRecent> recents = folderService.listRecents(principal.getId(), workspaceId);
        List<java.util.Map<String, Object>> result = recents.stream()
                .map(recent -> java.util.Map.<String, Object>of(
                        "id", recent.getFolder().getUuid(),
                        "name", recent.getFolder().getName(),
                        "parentId", recent.getFolder().getParent() != null ? recent.getFolder().getParent().getUuid() : "",
                        "accessedAt", recent.getAccessedAt().toString()
                ))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{workspaceId}/members")
    public ResponseEntity<ApiResponse<List<java.util.Map<String, Object>>>> listMembers(
            @PathVariable String workspaceId) {
        List<UserWorkspaceRole> members = workspaceService.getWorkspaceMembersByUuid(workspaceId);
        List<java.util.Map<String, Object>> result = members.stream()
                .map(uwr -> java.util.Map.<String, Object>of(
                        "id", uwr.getUser().getUuid(),
                        "firstName", uwr.getUser().getFirstName(),
                        "lastName", uwr.getUser().getLastName(),
                        "email", uwr.getUser().getEmail()
                ))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
