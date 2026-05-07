package com.cms.dto.audit;

import lombok.Data;

@Data
public class AuditSearchRequest {
    private String query;
    private String category;
    private String eventType;
    private Long userId;
    private String outcome;
    private Long workspaceId;
    private String dateFrom;
    private String dateTo;
    private Integer page = 0;
    private Integer size = 20;
}
