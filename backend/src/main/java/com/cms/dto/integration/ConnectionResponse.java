package com.cms.dto.integration;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConnectionResponse {
    private String id;
    private String provider;
    private String providerAccountId;
    private String status;
    private Instant connectedAt;
    private Instant lastUsedAt;
}
