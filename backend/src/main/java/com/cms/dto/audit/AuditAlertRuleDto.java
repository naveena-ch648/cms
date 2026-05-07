package com.cms.dto.audit;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuditAlertRuleDto {
    private String id;
    private String name;
    private String description;
    private String eventType;
    private int thresholdCount;
    private int timeWindowMinutes;
    private boolean enabled;
    private String createdAt;
}
