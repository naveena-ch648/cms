package com.cms.controller;

import com.cms.dto.ApiResponse;
import com.cms.dto.admin.*;
import com.cms.entity.User;
import com.cms.middleware.TenantContext;
import com.cms.security.UserPrincipal;
import com.cms.service.AdminAnalyticsService;
import com.cms.service.StorageQuotaService;
import com.cms.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final AdminAnalyticsService adminAnalyticsService;
    private final StorageQuotaService storageQuotaService;
    private final UserService userService;

    @GetMapping("/analytics")
    @PreAuthorize("hasPermission(null, 'view-audit-log')")
    public ResponseEntity<ApiResponse<AdminAnalyticsResponse>> getAnalytics(
            @RequestParam(defaultValue = "30") int days) {
        if (days < 1 || days > 90) {
            days = 30;
        }
        Long orgId = TenantContext.getCurrentTenant();
        AdminAnalyticsResponse response = adminAnalyticsService.getAnalytics(orgId, days);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/storage-quota")
    @PreAuthorize("hasPermission(null, 'manage-policies')")
    public ResponseEntity<ApiResponse<StorageQuotaDetailResponse>> getStorageQuota() {
        Long orgId = TenantContext.getCurrentTenant();
        StorageQuotaDetailResponse response = storageQuotaService.getQuotaDetail(orgId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/storage-quota")
    @PreAuthorize("hasPermission(null, 'manage-policies')")
    public ResponseEntity<ApiResponse<StorageQuotaDetailResponse>> updateStorageQuota(
            @RequestBody StorageQuotaUpdateRequest request) {
        Long orgId = TenantContext.getCurrentTenant();
        StorageQuotaDetailResponse response = storageQuotaService.updateQuota(orgId, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/users/bulk-action")
    @PreAuthorize("hasPermission(null, 'manage-users')")
    public ResponseEntity<ApiResponse<BulkUserActionResponse>> bulkUserAction(
            @Valid @RequestBody BulkUserActionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        if (request.getAction() == BulkUserActionRequest.BulkAction.CHANGE_ROLE
                && (request.getRoleId() == null || request.getRoleId().isBlank())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("VALIDATION_ERROR", "roleId is required for CHANGE_ROLE action"));
        }

        List<BulkUserActionResponse.UserActionResult> results = new ArrayList<>();
        int successful = 0;
        int failed = 0;

        for (String userId : request.getUserIds()) {
            try {
                switch (request.getAction()) {
                    case CHANGE_ROLE -> {
                        userService.changeRole(userId, request.getRoleId());
                    }
                    case ACTIVATE -> {
                        userService.activate(userId);
                    }
                    case DEACTIVATE -> {
                        userService.deactivateWithSelfCheck(userId, principal.getId());
                    }
                }
                results.add(BulkUserActionResponse.UserActionResult.builder()
                        .userId(userId)
                        .status("SUCCESS")
                        .build());
                successful++;
            } catch (Exception e) {
                results.add(BulkUserActionResponse.UserActionResult.builder()
                        .userId(userId)
                        .status("FAILED")
                        .reason(e.getMessage())
                        .build());
                failed++;
                log.warn("Bulk action failed for user {}: {}", userId, e.getMessage());
            }
        }

        BulkUserActionResponse response = BulkUserActionResponse.builder()
                .totalRequested(request.getUserIds().size())
                .successful(successful)
                .failed(failed)
                .results(results)
                .build();

        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
