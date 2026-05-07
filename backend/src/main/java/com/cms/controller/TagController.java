package com.cms.controller;

import com.cms.dto.ApiResponse;
import com.cms.dto.metadata.BulkTagRequest;
import com.cms.dto.metadata.TagRequest;
import com.cms.dto.metadata.TagResponse;
import com.cms.entity.FileEntity;
import com.cms.security.UserPrincipal;
import com.cms.service.FileService;
import com.cms.service.TagService;
import com.cms.service.WorkspaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;
    private final FileService fileService;
    private final WorkspaceService workspaceService;

    @GetMapping("/api/v1/files/{fileId}/tags")
    public ResponseEntity<ApiResponse<List<TagResponse>>> getFileTags(
            @PathVariable String fileId,
            @AuthenticationPrincipal UserPrincipal principal) {

        FileEntity file = fileService.getByUuid(fileId);
        verifyFileReadAccess(file, principal);

        List<TagResponse> tags = tagService.getFileTags(file.getId());
        return ResponseEntity.ok(ApiResponse.ok(tags));
    }

    @PostMapping("/api/v1/files/{fileId}/tags")
    public ResponseEntity<ApiResponse<Object>> addTags(
            @PathVariable String fileId,
            @Valid @RequestBody TagRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        FileEntity file = fileService.getByUuid(fileId);
        verifyFileWriteAccess(file, principal);

        List<TagResponse> tags = tagService.addTags(
                file.getId(), file.getWorkspace().getId(), principal.getId(), request.getTags());

        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "added", request.getTags().size(),
                "tags", tags
        )));
    }

    @DeleteMapping("/api/v1/files/{fileId}/tags/{tagName}")
    public ResponseEntity<Void> removeTag(
            @PathVariable String fileId,
            @PathVariable String tagName,
            @AuthenticationPrincipal UserPrincipal principal) {

        FileEntity file = fileService.getByUuid(fileId);
        verifyFileWriteAccess(file, principal);

        tagService.removeTag(file.getId(), tagName);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/workspaces/{workspaceId}/tags/autocomplete")
    public ResponseEntity<ApiResponse<List<String>>> autocomplete(
            @PathVariable String workspaceId,
            @RequestParam(defaultValue = "") String prefix,
            @RequestParam(defaultValue = "10") int limit,
            @AuthenticationPrincipal UserPrincipal principal) {

        Long wsId = workspaceService.getByUuid(workspaceId).getId();
        verifyWorkspaceMember(wsId, principal);

        List<String> suggestions = tagService.autocomplete(wsId, prefix, limit);
        return ResponseEntity.ok(ApiResponse.ok(suggestions));
    }

    @PostMapping("/api/v1/files/bulk-tags")
    public ResponseEntity<ApiResponse<Object>> bulkAddTags(
            @Valid @RequestBody BulkTagRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        List<Long> fileIds = request.getFileIds().stream()
                .map(uuid -> {
                    FileEntity file = fileService.getByUuid(uuid);
                    verifyFileWriteAccess(file, principal);
                    return file.getId();
                })
                .toList();

        FileEntity firstFile = fileService.getByUuid(request.getFileIds().get(0));
        tagService.bulkAddTags(fileIds, firstFile.getWorkspace().getId(), principal.getId(), request.getTags());

        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "totalFiles", fileIds.size(),
                "updated", fileIds.size(),
                "failed", 0,
                "errors", List.of()
        )));
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

    private void verifyWorkspaceMember(Long workspaceId, UserPrincipal principal) {
        if (!workspaceService.isWorkspaceMember(workspaceId, principal.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Not a workspace member");
        }
    }
}
