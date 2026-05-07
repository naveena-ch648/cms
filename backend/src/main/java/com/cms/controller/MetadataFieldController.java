package com.cms.controller;

import com.cms.dto.ApiResponse;
import com.cms.dto.metadata.MetadataFieldRequest;
import com.cms.dto.metadata.MetadataFieldResponse;
import com.cms.security.UserPrincipal;
import com.cms.service.MetadataFieldService;
import com.cms.service.WorkspaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/metadata-fields")
@RequiredArgsConstructor
public class MetadataFieldController {

    private final MetadataFieldService metadataFieldService;
    private final WorkspaceService workspaceService;

    @PostMapping
    public ResponseEntity<ApiResponse<MetadataFieldResponse>> createField(
            @PathVariable String workspaceId,
            @Valid @RequestBody MetadataFieldRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        Long wsId = resolveWorkspaceId(workspaceId);
        verifyWorkspaceAdmin(wsId, principal);

        MetadataFieldResponse response = metadataFieldService.createField(wsId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MetadataFieldResponse>>> listFields(
            @PathVariable String workspaceId,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            @AuthenticationPrincipal UserPrincipal principal) {

        Long wsId = resolveWorkspaceId(workspaceId);
        verifyWorkspaceMember(wsId, principal);

        List<MetadataFieldResponse> fields = metadataFieldService.listFields(wsId, includeDeleted);
        return ResponseEntity.ok(ApiResponse.ok(fields));
    }

    @PutMapping("/{fieldId}")
    public ResponseEntity<ApiResponse<MetadataFieldResponse>> updateField(
            @PathVariable String workspaceId,
            @PathVariable String fieldId,
            @Valid @RequestBody MetadataFieldRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        Long wsId = resolveWorkspaceId(workspaceId);
        verifyWorkspaceAdmin(wsId, principal);

        MetadataFieldResponse response = metadataFieldService.updateField(fieldId, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @DeleteMapping("/{fieldId}")
    public ResponseEntity<Void> deleteField(
            @PathVariable String workspaceId,
            @PathVariable String fieldId,
            @AuthenticationPrincipal UserPrincipal principal) {

        Long wsId = resolveWorkspaceId(workspaceId);
        verifyWorkspaceAdmin(wsId, principal);

        metadataFieldService.deleteField(fieldId);
        return ResponseEntity.noContent().build();
    }

    private Long resolveWorkspaceId(String workspaceUuid) {
        return workspaceService.getByUuid(workspaceUuid).getId();
    }

    private void verifyWorkspaceAdmin(Long workspaceId, UserPrincipal principal) {
        if (!workspaceService.isWorkspaceAdmin(workspaceId, principal.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Only workspace admins can manage metadata fields");
        }
    }

    private void verifyWorkspaceMember(Long workspaceId, UserPrincipal principal) {
        if (!workspaceService.isWorkspaceMember(workspaceId, principal.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("You are not a member of this workspace");
        }
    }
}
