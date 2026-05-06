package com.cms.dto.file;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StorageQuotaDto {

    private Long maxStorageBytes;
    private Long usedStorageBytes;
    private Long maxFileSizeBytes;
    private Double usedPercentage;
    private Integer trashRetentionDays;

    public static StorageQuotaDto from(com.cms.entity.StorageQuota quota) {
        double percentage = quota.getMaxStorageBytes() > 0
                ? (double) quota.getUsedStorageBytes() / quota.getMaxStorageBytes() * 100.0
                : 0.0;
        return StorageQuotaDto.builder()
                .maxStorageBytes(quota.getMaxStorageBytes())
                .usedStorageBytes(quota.getUsedStorageBytes())
                .maxFileSizeBytes(quota.getMaxFileSizeBytes())
                .usedPercentage(Math.round(percentage * 10.0) / 10.0)
                .trashRetentionDays(quota.getTrashRetentionDays())
                .build();
    }
}
