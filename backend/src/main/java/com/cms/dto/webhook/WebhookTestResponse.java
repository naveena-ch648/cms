package com.cms.dto.webhook;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookTestResponse {
    private boolean delivered;
    private Integer responseStatus;
    private Integer responseTimeMs;
    private String responseBody;
    private String error;
}
