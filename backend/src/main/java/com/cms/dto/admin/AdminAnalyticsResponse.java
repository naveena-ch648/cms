package com.cms.dto.admin;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminAnalyticsResponse {

    private Summary summary;
    private List<RoleDistributionItem> roleDistribution;
    private List<TrendItem> uploadTrend;
    private List<StorageTrendItem> storageTrend;
    private List<TopActiveUser> topActiveUsers;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Summary {
        private long totalUsers;
        private long activeUsers;
        private long inactiveUsers;
        private long lockedUsers;
        private long totalFiles;
        private long totalStorageUsedBytes;
        private long totalStorageMaxBytes;
        private double storageUsedPercent;
        private long totalWorkspaces;
        private long activeUsersLast30Days;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RoleDistributionItem {
        private String roleName;
        private long userCount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TrendItem {
        private String date;
        private long count;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StorageTrendItem {
        private String date;
        private long totalBytes;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TopActiveUser {
        private String userId;
        private String name;
        private long actionCount;
    }
}
