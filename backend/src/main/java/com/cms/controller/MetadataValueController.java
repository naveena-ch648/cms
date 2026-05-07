package com.cms.controller;

import com.cms.dto.ApiResponse;
import com.cms.dto.metadata.BulkMetadataRequest;
import com.cms.dto.metadata.MetadataValueRequest;
import com.cms.dto.metadata.MetadataValueResponse;
import com.cms.entity.FileEntity;
import com.cms.security.UserPrincipal;
import com.cms.service.FileService;
import com.cms.service.MetadataValueService;
import com.cms.service.WorkspaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class MetadataValueController {

    private final MetadataValueService metadataValueService;
    private final FileService fileService;
    private final WorkspaceService workspaceService;

    @GetMapping("/{fileId}/metadata")
    public ResponseEntity<ApiResponse<List<MetadataValueResponse>>> getFileMetadata(
            @PathVariable String fileId,
            @AuthenticationPrincipal UserPrincipal principal) {

        FileEntity file = fileService.getByUuid(fileId);
        verifyFileReadAccess(file, principal);

        List<MetadataValueResponse> values = metadataValueService.getFileMetadata(file.getId());
        return ResponseEntity.ok(ApiResponse.ok(values));
    }

    @PutMapping("/{fileId}/metadata")
    public ResponseEntity<ApiResponse<List<MetadataValueResponse>>> updateFileMetadata(
            @PathVariable String fileId,
            @Valid @RequestBody MetadataValueRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        FileEntity file = fileService.getByUuid(fileId);
        verifyFileWriteAccess(file, principal);

        List<MetadataValueResponse> values = metadataValueService.updateFileMetadata(file.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok(values));
    }

    @DeleteMapping("/{fileId}/metadata/{fieldId}")
    public ResponseEntity<Void> deleteFieldValue(
            @PathVariable String fileId,
            @PathVariable String fieldId,
            @AuthenticationPrincipal UserPrincipal principal) {

        FileEntity file = fileService.getByUuid(fileId);
        verifyFileWriteAccess(file, principal);

        metadataValueService.deleteFieldValue(file.getId(), fieldId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/bulk-metadata")
    public ResponseEntity<ApiResponse<Object>> bulkUpdateMetadata(
            @Valid @RequestBody BulkMetadataRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        List<Long> fileIds = request.getFileIds().stream()
                .map(uuid -> {
                    FileEntity file = fileService.getByUuid(uuid);
                    verifyFileWriteAccess(file, principal);
                    return file.getId();
                })
                .toList();

        metadataValueService.bulkUpdateMetadata(fileIds, request.getValues());

        return ResponseEntity.ok(ApiResponse.ok(
                java.util.Map.of("totalFiles", fileIds.size(), "updated", fileIds.size(), "failed", 0)
        ));
    }

    private void verifyFileReadAccess(FileEntity file, UserPrincipal principal) {
        if (!workspaceService.isWorkspaceMember(file.getWorkspace().getId(), principal.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("No read access to this file");
        }
    }

    private void verifyFileWriteAccess(FileEntity file, UserPrincipal principal) {
        if (!workspaceService.isWorkspaceMember(file.getWorkspace().getId(), principal.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("No write access to this file");
        }
    }
}
