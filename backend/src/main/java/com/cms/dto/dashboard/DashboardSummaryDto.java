package com.cms.dto.dashboard;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardSummaryDto {

    private int recentFilesCount;
    private long unreadNotifications;
    private long pendingApprovals;
    private long storageUsedBytes;
    private long storageMaxBytes;
    private double storagePercentage;
    private long activeAlertsCount;
}
