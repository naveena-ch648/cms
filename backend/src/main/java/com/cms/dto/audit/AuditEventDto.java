package com.cms.dto.audit;

import com.cms.entity.AuditCategory;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuditEventDto {
    private Long id;
    private Long userId;
    private String actorName;
    private String eventType;
    private AuditCategory category;
    private String resourceType;
    private Long resourceId;
    private String resourceName;
    private String outcome;
    private String ipAddress;
    private String createdAt;
}
