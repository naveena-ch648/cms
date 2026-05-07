package com.cms.service;

import com.cms.dto.admin.AdminAnalyticsResponse;
import com.cms.entity.FileEntity;
import com.cms.entity.StorageQuota;
import com.cms.entity.User;
import com.cms.entity.Workspace;
import com.cms.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminAnalyticsService {

    private static final String CACHE_KEY_PREFIX = "admin:analytics:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final UserRepository userRepository;
    private final FileRepository fileRepository;
    private final WorkspaceRepository workspaceRepository;
    private final StorageQuotaRepository storageQuotaRepository;
    private final UserOrganizationRoleRepository userOrgRoleRepository;
    private final AuditEventRepository auditEventRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public AdminAnalyticsResponse getAnalytics(Long organizationId, int days) {
        String cacheKey = CACHE_KEY_PREFIX + organizationId + ":" + days;

        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return objectMapper.readValue(cached, AdminAnalyticsResponse.class);
            }
        } catch (Exception e) {
            log.warn("Failed to read analytics cache: {}", e.getMessage());
        }

        AdminAnalyticsResponse response = buildAnalytics(organizationId, days);

        try {
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(cacheKey, json, CACHE_TTL);
        } catch (Exception e) {
            log.warn("Failed to cache analytics: {}", e.getMessage());
        }

        return response;
    }

    private AdminAnalyticsResponse buildAnalytics(Long organizationId, int days) {
        // Summary
        long totalUsers = userRepository.countByOrganizationId(organizationId);
        long activeUsers = userRepository.countByOrganizationIdAndStatus(organizationId, User.UserStatus.ACTIVE);
        long inactiveUsers = userRepository.countByOrganizationIdAndStatus(organizationId, User.UserStatus.INACTIVE);
        long lockedUsers = userRepository.countByOrganizationIdAndStatus(organizationId, User.UserStatus.LOCKED);
        long totalFiles = fileRepository.countByOrganizationIdAndStatusNot(organizationId, FileEntity.FileStatus.DELETED);
        long totalWorkspaces = workspaceRepository.countByOrganizationIdAndStatusNot(organizationId, Workspace.WorkspaceStatus.DELETED);

        Instant thirtyDaysAgo = Instant.now().minus(Duration.ofDays(30));
        long activeUsersLast30Days = userRepository.countActiveUsersSince(organizationId, thirtyDaysAgo);

        long storageUsed = 0;
        long storageMax = 0;
        try {
            StorageQuota quota = storageQuotaRepository.findByOrganizationId(organizationId).orElse(null);
            if (quota != null) {
                storageUsed = quota.getUsedStorageBytes();
                storageMax = quota.getMaxStorageBytes();
            }
        } catch (Exception e) {
            log.warn("Failed to get storage quota for org {}: {}", organizationId, e.getMessage());
        }

        double storagePercent = storageMax > 0
                ? Math.round((double) storageUsed / storageMax * 1000.0) / 10.0
                : 0.0;

        AdminAnalyticsResponse.Summary summary = AdminAnalyticsResponse.Summary.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .inactiveUsers(inactiveUsers)
                .lockedUsers(lockedUsers)
                .totalFiles(totalFiles)
                .totalStorageUsedBytes(storageUsed)
                .totalStorageMaxBytes(storageMax)
                .storageUsedPercent(storagePercent)
                .totalWorkspaces(totalWorkspaces)
                .activeUsersLast30Days(activeUsersLast30Days)
                .build();

        // Role distribution
        List<Object[]> roleDistRaw = userOrgRoleRepository.countByOrganizationIdGroupByRole(organizationId);
        List<AdminAnalyticsResponse.RoleDistributionItem> roleDistribution = roleDistRaw.stream()
                .map(row -> AdminAnalyticsResponse.RoleDistributionItem.builder()
                        .roleName((String) row[0])
                        .userCount((Long) row[1])
                        .build())
                .toList();

        // Upload trend
        Instant since = Instant.now().minus(Duration.ofDays(days));
        List<Object[]> uploadRaw = auditEventRepository.countByOrgAndEventTypeGroupByDate(
                organizationId, "FILE_UPLOADED", since);
        List<AdminAnalyticsResponse.TrendItem> uploadTrend = uploadRaw.stream()
                .map(row -> AdminAnalyticsResponse.TrendItem.builder()
                        .date(row[0].toString())
                        .count((Long) row[1])
                        .build())
                .toList();

        // Storage trend (approximate: use cumulative file sizes by upload date)
        // For simplicity, reuse upload trend dates with cumulative storage
        List<AdminAnalyticsResponse.StorageTrendItem> storageTrend = buildStorageTrend(organizationId, days);

        // Top active users
        List<Object[]> topUsersRaw = auditEventRepository.findTopActiveUsers(
                organizationId, since, PageRequest.of(0, 10));
        List<AdminAnalyticsResponse.TopActiveUser> topActiveUsers = topUsersRaw.stream()
                .map(row -> AdminAnalyticsResponse.TopActiveUser.builder()
                        .userId(row[0].toString())
                        .name(row[1] + " " + row[2])
                        .actionCount((Long) row[3])
                        .build())
                .toList();

        return AdminAnalyticsResponse.builder()
                .summary(summary)
                .roleDistribution(roleDistribution)
                .uploadTrend(uploadTrend)
                .storageTrend(storageTrend)
                .topActiveUsers(topActiveUsers)
                .build();
    }

    private List<AdminAnalyticsResponse.StorageTrendItem> buildStorageTrend(Long organizationId, int days) {
        // Build a simple storage trend based on current usage
        // In production, this could use a storage_snapshots table
        long currentUsage = 0;
        try {
            StorageQuota quota = storageQuotaRepository.findByOrganizationId(organizationId).orElse(null);
            if (quota != null) {
                currentUsage = quota.getUsedStorageBytes();
            }
        } catch (Exception e) {
            // ignore
        }

        List<AdminAnalyticsResponse.StorageTrendItem> trend = new ArrayList<>();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            trend.add(AdminAnalyticsResponse.StorageTrendItem.builder()
                    .date(date.format(DateTimeFormatter.ISO_LOCAL_DATE))
                    .totalBytes(currentUsage)
                    .build());
        }
        return trend;
    }
}
