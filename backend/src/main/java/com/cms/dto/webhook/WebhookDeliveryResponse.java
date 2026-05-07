package com.cms.dto.webhook;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookDeliveryResponse {
    private String id;
    private String eventType;
    private String eventId;
    private String status;
    private Integer responseStatus;
    private Integer responseTimeMs;
    private int attemptNumber;
    private Instant deliveredAt;
    private Instant createdAt;
}
