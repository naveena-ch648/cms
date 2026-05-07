package com.cms.dto.integration;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncLinkUpdateRequest {
    private String direction;
    private Integer syncIntervalMinutes;
    private String status;
}
