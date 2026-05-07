package com.cms.dto.admin;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StorageQuotaUpdateRequest {

    private Long maxStorageBytes;
    private Long maxFileSizeBytes;
    private List<String> allowedExtensions;
    private List<String> blockedExtensions;
    private Integer trashRetentionDays;
}
