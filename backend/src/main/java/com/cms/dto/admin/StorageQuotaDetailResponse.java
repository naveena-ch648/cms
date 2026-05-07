package com.cms.dto.admin;

import com.cms.entity.StorageQuota;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StorageQuotaDetailResponse {

    private Long maxStorageBytes;
    private Long usedStorageBytes;
    private Long maxFileSizeBytes;
    private List<String> allowedExtensions;
    private List<String> blockedExtensions;
    private Integer trashRetentionDays;
    private Double usedPercent;
    private String warning;

    public static StorageQuotaDetailResponse from(StorageQuota quota, ObjectMapper objectMapper) {
        double percent = quota.getMaxStorageBytes() > 0
                ? (double) quota.getUsedStorageBytes() / quota.getMaxStorageBytes() * 100.0
                : 0.0;
        percent = Math.round(percent * 10.0) / 10.0;

        String warningMsg = null;
        if (quota.getUsedStorageBytes() > quota.getMaxStorageBytes()) {
            warningMsg = "Storage quota is below current usage. New uploads will be blocked until usage is reduced.";
        }

        return StorageQuotaDetailResponse.builder()
                .maxStorageBytes(quota.getMaxStorageBytes())
                .usedStorageBytes(quota.getUsedStorageBytes())
                .maxFileSizeBytes(quota.getMaxFileSizeBytes())
                .allowedExtensions(parseJsonList(quota.getAllowedExtensions(), objectMapper))
                .blockedExtensions(parseJsonList(quota.getBlockedExtensions(), objectMapper))
                .trashRetentionDays(quota.getTrashRetentionDays())
                .usedPercent(percent)
                .warning(warningMsg)
                .build();
    }

    private static List<String> parseJsonList(String json, ObjectMapper objectMapper) {
        if (json == null || json.isBlank() || "null".equals(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return null;
        }
    }
}
