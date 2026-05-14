package com.cms.controller;

import com.cms.dto.ApiResponse;
import com.cms.dto.fileshare.CreateFileShareRequest;
import com.cms.dto.fileshare.FileShareResponse;
import com.cms.dto.fileshare.SharedWithMeResponse;
import com.cms.dto.fileshare.UpdateFileShareRequest;
import com.cms.entity.FileShare;
import com.cms.security.UserPrincipal;
import com.cms.service.FileShareService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class FileShareController {

    private final FileShareService fileShareService;

    // ── Share a file ──────────────────────────────────────────────────────────

    @PostMapping("/api/v1/files/{fileUuid}/shares")
    public ResponseEntity<ApiResponse<FileShareResponse>> shareFile(
            @PathVariable String fileUuid,
            @Valid @RequestBody CreateFileShareRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        FileShare share = fileShareService.shareFile(fileUuid, principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(FileShareResponse.from(share)));
    }

    // ── List shares for a file (who has access) ───────────────────────────────

    @GetMapping("/api/v1/files/{fileUuid}/shares")
    public ResponseEntity<ApiResponse<List<FileShareResponse>>> listShares(
            @PathVariable String fileUuid,
            @AuthenticationPrincipal UserPrincipal principal) {

        List<FileShare> shares = fileShareService.listSharesForFile(fileUuid, principal.getId());
        List<FileShareResponse> response = shares.stream()
                .map(FileShareResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // ── Update a share (change permission / options) ──────────────────────────

    @PatchMapping("/api/v1/file-shares/{shareUuid}")
    public ResponseEntity<ApiResponse<FileShareResponse>> updateShare(
            @PathVariable String shareUuid,
            @RequestBody UpdateFileShareRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        FileShare share = fileShareService.updateShare(shareUuid, principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok(FileShareResponse.from(share)));
    }

    // ── Revoke a share ────────────────────────────────────────────────────────

    @DeleteMapping("/api/v1/file-shares/{shareUuid}")
    public ResponseEntity<ApiResponse<Void>> revokeShare(
            @PathVariable String shareUuid,
            @AuthenticationPrincipal UserPrincipal principal) {

        fileShareService.revokeShare(shareUuid, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // ── Shared-with-me (receiver's Shared Files page) ─────────────────────────

    @GetMapping("/api/v1/shared-with-me")
    public ResponseEntity<ApiResponse<Page<SharedWithMeResponse>>> sharedWithMe(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {

        Pageable pageable = PageRequest.of(page, size);
        Page<FileShare> shares = fileShareService.listSharedWithMe(principal.getId(), pageable);
        Page<SharedWithMeResponse> response = shares.map(SharedWithMeResponse::from);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
