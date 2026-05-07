package com.cms.controller;

import com.cms.annotation.Audited;
import com.cms.dto.ApiResponse;
import com.cms.dto.sharing.*;
import com.cms.entity.AuditCategory;
import com.cms.entity.AuditEventType;
import com.cms.entity.SharedLink;
import com.cms.entity.SharedLinkAccess;
import com.cms.security.UserPrincipal;
import com.cms.service.SharedLinkService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class SharedLinkController {

    private final SharedLinkService sharedLinkService;

    @Value("${app.base-url:http://localhost:3000}")
    private String baseUrl;

    // --- Authenticated endpoints ---

    @PostMapping("/api/v1/workspaces/{workspaceId}/share-links")
    @Audited(event = AuditEventType.LINK_CREATED, category = AuditCategory.SHARING, resourceType = "share_link")
    public ResponseEntity<ApiResponse<ShareLinkResponse>> createLink(
            @PathVariable String workspaceId,
            @Valid @RequestBody CreateShareLinkRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        SharedLink link = sharedLinkService.createLink(request, principal.getId(), workspaceId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(ShareLinkResponse.from(link, baseUrl)));
    }

    @GetMapping("/api/v1/workspaces/{workspaceId}/share-links")
    public ResponseEntity<ApiResponse<Page<ShareLinkResponse>>> listLinks(
            @PathVariable String workspaceId,
            @RequestParam(required = false) SharedLink.LinkStatus status,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal UserPrincipal principal) {
        Page<SharedLink> links = sharedLinkService.listLinks(principal.getId(), workspaceId, status, pageable);
        Page<ShareLinkResponse> response = links.map(link -> ShareLinkResponse.from(link, baseUrl));
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PatchMapping("/api/v1/share-links/{uuid}")
    public ResponseEntity<ApiResponse<ShareLinkResponse>> updateLink(
            @PathVariable String uuid,
            @Valid @RequestBody UpdateShareLinkRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        SharedLink link = sharedLinkService.updateLink(uuid, principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok(ShareLinkResponse.from(link, baseUrl)));
    }

    @DeleteMapping("/api/v1/share-links/{uuid}")
    @Audited(event = AuditEventType.LINK_REVOKED, category = AuditCategory.SHARING, resourceType = "share_link")
    public ResponseEntity<ApiResponse<Void>> revokeLink(
            @PathVariable String uuid,
            @AuthenticationPrincipal UserPrincipal principal) {
        sharedLinkService.revokeLink(uuid, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/api/v1/share-links/{uuid}/accesses")
    public ResponseEntity<ApiResponse<Page<ShareLinkAccessResponse>>> getAccesses(
            @PathVariable String uuid,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal UserPrincipal principal) {
        Page<SharedLinkAccess> accesses = sharedLinkService.getAccessLog(uuid, principal.getId(), pageable);
        Page<ShareLinkAccessResponse> response = accesses.map(a ->
                ShareLinkAccessResponse.builder()
                        .accessedAt(a.getAccessedAt())
                        .ipAddress(a.getIpAddress())
                        .userAgent(a.getUserAgent())
                        .build());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // --- Public endpoints (no authentication required) ---

    @GetMapping("/api/share/{token}")
    public ResponseEntity<ApiResponse<PublicShareLinkResponse>> accessLink(
            @PathVariable String token,
            HttpServletRequest request) {
        SharedLink link = sharedLinkService.recordAccess(
                token,
                request.getRemoteAddr(),
                request.getHeader("User-Agent"));

        PublicShareLinkResponse response = buildPublicResponse(link);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/api/share/{token}/verify")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyPassword(
            @PathVariable String token,
            @Valid @RequestBody VerifyPasswordRequest passwordRequest) {
        boolean valid = sharedLinkService.verifyPassword(token, passwordRequest.getPassword());
        if (!valid) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("INVALID_PASSWORD", "Incorrect password"));
        }
        return ResponseEntity.ok(ApiResponse.ok(Map.of("verified", true)));
    }

    @GetMapping("/api/share/{token}/download")
    public ResponseEntity<ApiResponse<Map<String, String>>> getDownloadUrl(@PathVariable String token) {
        String url = sharedLinkService.generateDownloadUrl(token);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("downloadUrl", url)));
    }

    private PublicShareLinkResponse buildPublicResponse(SharedLink link) {
        PublicShareLinkResponse.PublicShareLinkResponseBuilder builder = PublicShareLinkResponse.builder()
                .resourceType(link.getResourceType().name())
                .allowDownload(link.isAllowDownload())
                .watermarkEnabled(link.isWatermarkEnabled())
                .requiresPassword(link.getPasswordHash() != null);

        if (link.getResourceType() == SharedLink.ResourceType.FILE && link.getFile() != null) {
            builder.resourceName(link.getFile().getName())
                    .mimeType(link.getFile().getMimeType())
                    .size(link.getFile().getSizeBytes());
        } else if (link.getFolder() != null) {
            builder.resourceName(link.getFolder().getName());
        }

        return builder.build();
    }
}
