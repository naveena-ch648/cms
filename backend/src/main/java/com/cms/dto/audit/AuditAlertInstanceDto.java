package com.cms.dto.audit;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuditAlertInstanceDto {
    private String id;
    private String ruleName;
    private String ruleId;
    private String triggeredByUser;
    private int eventCount;
    private String windowStart;
    private String windowEnd;
    private boolean acknowledged;
    private String acknowledgedBy;
    private String acknowledgedAt;
    private String createdAt;
}
