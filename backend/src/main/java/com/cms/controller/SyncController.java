package com.cms.controller;

import com.cms.dto.ApiResponse;
import com.cms.dto.integration.*;
import com.cms.security.UserPrincipal;
import com.cms.service.SyncService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/integrations/sync-links")
@RequiredArgsConstructor
@Slf4j
public class SyncController {

    private final SyncService syncService;

    @PostMapping
    public ResponseEntity<ApiResponse<SyncLinkResponse>> createSyncLink(
            @Valid @RequestBody SyncLinkRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        SyncLinkResponse response = syncService.createSyncLink(request, user);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SyncLinkResponse>>> listSyncLinks(
            @AuthenticationPrincipal UserPrincipal user) {
        List<SyncLinkResponse> links = syncService.listSyncLinks(user);
        return ResponseEntity.ok(ApiResponse.ok(links));
    }

    @PutMapping("/{syncLinkId}")
    public ResponseEntity<ApiResponse<SyncLinkResponse>> updateSyncLink(
            @PathVariable String syncLinkId,
            @RequestBody SyncLinkUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        SyncLinkResponse response = syncService.updateSyncLink(syncLinkId, request, user);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @DeleteMapping("/{syncLinkId}")
    public ResponseEntity<ApiResponse<Void>> deleteSyncLink(
            @PathVariable String syncLinkId,
            @AuthenticationPrincipal UserPrincipal user) {
        syncService.deleteSyncLink(syncLinkId, user);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/{syncLinkId}/jobs")
    public ResponseEntity<ApiResponse<Page<SyncJobResponse>>> getSyncJobs(
            @PathVariable String syncLinkId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<SyncJobResponse> jobs = syncService.getSyncJobs(syncLinkId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.ok(jobs));
    }
}
