package com.cms.dto.integration;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncLinkResponse {
    private String id;
    private String folderId;
    private String folderName;
    private String externalFolderName;
    private String provider;
    private String direction;
    private int syncIntervalMinutes;
    private String status;
    private Instant lastSyncAt;
    private Instant nextSyncAt;
}
