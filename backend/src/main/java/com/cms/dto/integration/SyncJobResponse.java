package com.cms.dto.integration;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncJobResponse {
    private String id;
    private String status;
    private String direction;
    private int itemsSynced;
    private int itemsFailed;
    private int itemsConflicted;
    private long bytesTransferred;
    private Instant startedAt;
    private Instant completedAt;
}
