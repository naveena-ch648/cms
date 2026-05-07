package com.cms.service;

import com.cms.dto.dashboard.*;
import com.cms.entity.FileEntity;
import com.cms.entity.SharedLink;
import com.cms.entity.StorageQuota;
import com.cms.entity.User;
import com.cms.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private static final String DASHBOARD_SUMMARY_KEY_PREFIX = "dashboard:summary:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(2);

    private final FileRepository fileRepository;
    private final UserWorkspaceRoleRepository userWorkspaceRoleRepository;
    private final NotificationService notificationService;
    private final ActivityEventService activityEventService;
    private final StorageQuotaService storageQuotaService;
    private final AlertService alertService;
    private final SharedLinkRepository sharedLinkRepository;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final StringRedisTemplate redisTemplate;

    @Transactional(readOnly = true)
    public DashboardSummaryDto getSummary(Long userId, Long organizationId) {
        List<Long> workspaceIds = getUserWorkspaceIds(userId);

        long unreadNotifications = notificationService.getUnreadCount(userId);
        long activeAlerts = alertService.getActiveAlertCount(userId);
        long pendingApprovals = getPendingApprovalCount(userId);

        int recentFilesCount = 0;
        if (!workspaceIds.isEmpty()) {
            Page<FileEntity> recentPage = fileRepository.findByWorkspaceIdInAndStatusOrderByLastAccessedAtDesc(
                    workspaceIds, FileEntity.FileStatus.ACTIVE, PageRequest.of(0, 10));
            recentFilesCount = (int) recentPage.getTotalElements();
        }

        // Storage
        long storageUsedBytes = 0;
        long storageMaxBytes = 0;
        double storagePercentage = 0;
        try {
            StorageQuota quota = storageQuotaService.getQuotaForOrg(organizationId);
            storageUsedBytes = quota.getUsedStorageBytes();
            storageMaxBytes = quota.getMaxStorageBytes();
            storagePercentage = storageMaxBytes > 0
                    ? (double) storageUsedBytes / storageMaxBytes * 100
                    : 0;
        } catch (IllegalStateException e) {
            // No quota configured
        }

        return DashboardSummaryDto.builder()
                .recentFilesCount(recentFilesCount)
                .unreadNotifications(unreadNotifications)
                .pendingApprovals(pendingApprovals)
                .storageUsedBytes(storageUsedBytes)
                .storageMaxBytes(storageMaxBytes)
                .storagePercentage(storagePercentage)
                .activeAlertsCount(activeAlerts)
                .build();
    }

    @Transactional(readOnly = true)
    public List<RecentFileDto> getRecentFiles(Long userId, int limit) {
        List<Long> workspaceIds = getUserWorkspaceIds(userId);
        if (workspaceIds.isEmpty()) {
            return Collections.emptyList();
        }
        Page<FileEntity> recentFiles = fileRepository.findByWorkspaceIdInAndStatusOrderByLastAccessedAtDesc(
                workspaceIds, FileEntity.FileStatus.ACTIVE, PageRequest.of(0, limit));
        return recentFiles.getContent().stream()
                .map(RecentFileDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<ActivityEventDto> getActivityFeed(Long userId, Pageable pageable) {
        List<Long> workspaceIds = getUserWorkspaceIds(userId);
        if (workspaceIds.isEmpty()) {
            return Page.empty(pageable);
        }
        return activityEventService.getActivityFeed(workspaceIds, pageable);
    }

    @Transactional(readOnly = true)
    public List<SharedItemDto> getSharedItems(Long userId, String direction, int limit) {
        List<Long> workspaceIds = getUserWorkspaceIds(userId);
        if (workspaceIds.isEmpty()) {
            return Collections.emptyList();
        }

        if ("BY_ME".equalsIgnoreCase(direction)) {
            return workspaceIds.stream()
                    .flatMap(wsId -> sharedLinkRepository.findByCreatedByIdAndWorkspaceIdAndStatus(
                            userId, wsId, SharedLink.LinkStatus.ACTIVE, PageRequest.of(0, limit)).getContent().stream())
                    .limit(limit)
                    .map(SharedItemDto::fromSharedByMe)
                    .collect(Collectors.toList());
        }

        // Default: shared with me — using active links in user's workspaces
        return workspaceIds.stream()
                .flatMap(wsId -> sharedLinkRepository.findByWorkspaceIdAndStatus(
                        wsId, SharedLink.LinkStatus.ACTIVE, PageRequest.of(0, limit)).getContent().stream())
                .filter(link -> link.getCreatedBy() == null || !link.getCreatedBy().getId().equals(userId))
                .limit(limit)
                .map(link -> SharedItemDto.builder()
                        .id(link.getUuid())
                        .fileName(link.getFile() != null ? link.getFile().getName() : null)
                        .fileId(link.getFile() != null ? link.getFile().getUuid() : null)
                        .sharedBy(link.getCreatedBy() != null
                                ? link.getCreatedBy().getFirstName() + " " + link.getCreatedBy().getLastName() : null)
                        .sharedWith("Me")
                        .sharedAt(link.getCreatedAt())
                        .expiresAt(link.getExpiresAt())
                        .type("SHARED_WITH_ME")
                        .build())
                .collect(Collectors.toList());
    }

    public void invalidateSummaryCache(Long userId) {
        try {
            redisTemplate.delete(DASHBOARD_SUMMARY_KEY_PREFIX + userId);
        } catch (Exception e) {
            log.warn("Failed to invalidate dashboard cache for user {}: {}", userId, e.getMessage());
        }
    }

    private List<Long> getUserWorkspaceIds(Long userId) {
        return userWorkspaceRoleRepository.findWorkspaceIdsByUserId(userId);
    }

    private long getPendingApprovalCount(Long userId) {
        try {
            return approvalRequestRepository.findPendingByReviewerId(userId, PageRequest.of(0, 1))
                    .getTotalElements();
        } catch (Exception e) {
            log.warn("Failed to get pending approval count: {}", e.getMessage());
            return 0;
        }
    }
}
