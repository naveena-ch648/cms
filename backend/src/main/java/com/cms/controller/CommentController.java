package com.cms.controller;

import com.cms.dto.ApiResponse;
import com.cms.dto.preview.CommentDto;
import com.cms.entity.FileEntity;
import com.cms.entity.Folder;
import com.cms.entity.User;
import com.cms.repository.FileRepository;
import com.cms.repository.FolderRepository;
import com.cms.repository.UserRepository;
import com.cms.security.UserPrincipal;
import com.cms.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final FileRepository fileRepository;
    private final FolderRepository folderRepository;
    private final UserRepository userRepository;

    // === File Comments ===

    @GetMapping("/api/v1/files/{fileId}/comments")
    public ResponseEntity<ApiResponse<Page<CommentDto>>> getFileComments(
            @PathVariable String fileId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal UserPrincipal principal) {

        FileEntity file = fileRepository.findByUuid(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + fileId));

        int pageSize = Math.min(size, 100);
        Page<CommentDto> comments = commentService.getComments(file.getId(), PageRequest.of(page, pageSize));
        return ResponseEntity.ok(ApiResponse.ok(comments));
    }

    @GetMapping("/api/v1/files/{fileId}/comments/count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getFileCommentCount(
            @PathVariable String fileId,
            @AuthenticationPrincipal UserPrincipal principal) {

        FileEntity file = fileRepository.findByUuid(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + fileId));

        long count = commentService.getCommentCount(file.getId());
        return ResponseEntity.ok(ApiResponse.ok(Map.of("count", count)));
    }

    @PostMapping("/api/v1/files/{fileId}/comments")
    public ResponseEntity<ApiResponse<CommentDto>> createFileComment(
            @PathVariable String fileId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {

        FileEntity file = fileRepository.findByUuid(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + fileId));

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String content = body.get("content");
        String parentId = body.get("parentId");

        CommentDto comment = commentService.createComment(file, user, content, parentId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(comment));
    }

    @DeleteMapping("/api/v1/files/{fileId}/comments/{commentId}")
    public ResponseEntity<Void> deleteFileComment(
            @PathVariable String fileId,
            @PathVariable String commentId,
            @AuthenticationPrincipal UserPrincipal principal) {

        try {
            commentService.deleteComment(commentId, principal.getId());
            return ResponseEntity.noContent().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    // === Folder Comments ===

    @GetMapping("/api/v1/folders/{folderId}/comments")
    public ResponseEntity<ApiResponse<Page<CommentDto>>> getFolderComments(
            @PathVariable String folderId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal UserPrincipal principal) {

        Folder folder = folderRepository.findByUuid(folderId)
                .orElseThrow(() -> new IllegalArgumentException("Folder not found: " + folderId));

        int pageSize = Math.min(size, 100);
        Page<CommentDto> comments = commentService.getFolderComments(folder.getId(), PageRequest.of(page, pageSize));
        return ResponseEntity.ok(ApiResponse.ok(comments));
    }

    @GetMapping("/api/v1/folders/{folderId}/comments/count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getFolderCommentCount(
            @PathVariable String folderId,
            @AuthenticationPrincipal UserPrincipal principal) {

        Folder folder = folderRepository.findByUuid(folderId)
                .orElseThrow(() -> new IllegalArgumentException("Folder not found: " + folderId));

        long count = commentService.getFolderCommentCount(folder.getId());
        return ResponseEntity.ok(ApiResponse.ok(Map.of("count", count)));
    }

    @PostMapping("/api/v1/folders/{folderId}/comments")
    public ResponseEntity<ApiResponse<CommentDto>> createFolderComment(
            @PathVariable String folderId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {

        Folder folder = folderRepository.findByUuid(folderId)
                .orElseThrow(() -> new IllegalArgumentException("Folder not found: " + folderId));

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String content = body.get("content");
        String parentId = body.get("parentId");

        CommentDto comment = commentService.createFolderComment(folder, user, content, parentId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(comment));
    }

    @DeleteMapping("/api/v1/folders/{folderId}/comments/{commentId}")
    public ResponseEntity<Void> deleteFolderComment(
            @PathVariable String folderId,
            @PathVariable String commentId,
            @AuthenticationPrincipal UserPrincipal principal) {

        try {
            commentService.deleteComment(commentId, principal.getId());
            return ResponseEntity.noContent().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }
}
