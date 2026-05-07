package com.cms.controller;

import com.cms.dto.ApiResponse;
import com.cms.dto.dashboard.*;
import com.cms.entity.User;
import com.cms.security.UserPrincipal;
import com.cms.service.AlertService;
import com.cms.service.DashboardService;
import com.cms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final AlertService alertService;
    private final UserRepository userRepository;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DashboardSummaryDto>> getSummary(
            @AuthenticationPrincipal UserPrincipal principal) {
        DashboardSummaryDto summary = dashboardService.getSummary(
                principal.getId(), principal.getOrganizationId());
        return ResponseEntity.ok(ApiResponse.ok(summary));
    }

    @GetMapping("/recent-files")
    public ResponseEntity<ApiResponse<List<RecentFileDto>>> getRecentFiles(
            @RequestParam(defaultValue = "10") int limit,
            @AuthenticationPrincipal UserPrincipal principal) {
        int safeLimit = Math.min(limit, 50);
        List<RecentFileDto> files = dashboardService.getRecentFiles(principal.getId(), safeLimit);
        return ResponseEntity.ok(ApiResponse.ok(files));
    }

    @GetMapping("/activity")
    public ResponseEntity<ApiResponse<Page<ActivityEventDto>>> getActivity(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        int pageSize = Math.min(size, 50);
        Page<ActivityEventDto> activity = dashboardService.getActivityFeed(
                principal.getId(), PageRequest.of(page, pageSize));
        return ResponseEntity.ok(ApiResponse.ok(activity));
    }

    @GetMapping("/shared")
    public ResponseEntity<ApiResponse<List<SharedItemDto>>> getShared(
            @RequestParam(defaultValue = "WITH_ME") String direction,
            @RequestParam(defaultValue = "10") int limit,
            @AuthenticationPrincipal UserPrincipal principal) {
        int safeLimit = Math.min(limit, 50);
        List<SharedItemDto> items = dashboardService.getSharedItems(
                principal.getId(), direction, safeLimit);
        return ResponseEntity.ok(ApiResponse.ok(items));
    }

    @GetMapping("/alerts")
    public ResponseEntity<ApiResponse<List<AlertDto>>> getAlerts(
            @AuthenticationPrincipal UserPrincipal principal) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new IllegalStateException("User not found"));
        alertService.generateAlerts(user, principal.getOrganizationId());
        List<AlertDto> alerts = alertService.getActiveAlerts(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(alerts));
    }

    @PostMapping("/alerts/{alertId}/dismiss")
    public ResponseEntity<ApiResponse<Void>> dismissAlert(
            @PathVariable String alertId,
            @AuthenticationPrincipal UserPrincipal principal) {
        alertService.dismiss(alertId, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
