package com.cms.controller;

import com.cms.dto.ApiResponse;
import com.cms.dto.integration.*;
import com.cms.security.UserPrincipal;
import com.cms.service.IntegrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/integrations")
@RequiredArgsConstructor
@Slf4j
public class IntegrationController {

    private final IntegrationService integrationService;

    @GetMapping("/google-drive/connect")
    public ResponseEntity<ApiResponse<Map<String, String>>> getConnectUrl(
            @AuthenticationPrincipal UserPrincipal user) {
        String url = integrationService.generateAuthorizationUrl(user);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("authorizationUrl", url)));
    }

    @GetMapping("/google-drive/callback")
    public ResponseEntity<ApiResponse<ConnectionResponse>> handleCallback(
            @RequestParam String code,
            @RequestParam String state,
            @AuthenticationPrincipal UserPrincipal user) {
        ConnectionResponse response = integrationService.handleOAuthCallback(code, state, user);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/connections")
    public ResponseEntity<ApiResponse<List<ConnectionResponse>>> getConnections(
            @AuthenticationPrincipal UserPrincipal user) {
        List<ConnectionResponse> connections = integrationService.getUserConnections(user);
        return ResponseEntity.ok(ApiResponse.ok(connections));
    }

    @DeleteMapping("/connections/{connectionId}")
    public ResponseEntity<ApiResponse<Void>> disconnect(
            @PathVariable String connectionId,
            @AuthenticationPrincipal UserPrincipal user) {
        integrationService.disconnectConnection(connectionId, user);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/google-drive/browse")
    public ResponseEntity<ApiResponse<DriveBrowseResponse>> browseDrive(
            @RequestParam(required = false) String folderId,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String pageToken,
            @AuthenticationPrincipal UserPrincipal user) {
        DriveBrowseResponse response = integrationService.browseDrive(folderId, query, pageToken, user);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/google-drive/import")
    public ResponseEntity<ApiResponse<JobResponse>> importFromDrive(
            @Valid @RequestBody ImportRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        JobResponse response = integrationService.importFromDrive(request, user);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/google-drive/export")
    public ResponseEntity<ApiResponse<JobResponse>> exportToDrive(
            @Valid @RequestBody ExportRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        JobResponse response = integrationService.exportToDrive(request, user);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<ApiResponse<Map<String, String>>> getJobStatus(
            @PathVariable String jobId) {
        Map<String, String> status = integrationService.getJobStatus(jobId);
        return ResponseEntity.ok(ApiResponse.ok(status));
    }
}
