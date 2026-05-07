package com.cms.dto.webhook;

import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookResponse {
    private String id;
    private String name;
    private String url;
    private List<String> eventTypes;
    private String status;
    private int consecutiveFailures;
    private CreatedByInfo createdBy;
    private Instant createdAt;
    private Instant updatedAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreatedByInfo {
        private String id;
        private String name;
    }
}
