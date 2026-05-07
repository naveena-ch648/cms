package com.cms.dto.webhook;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookUpdateRequest {
    private String name;
    private String url;
    private String secret;
    private List<String> eventTypes;
    private String status;
}
