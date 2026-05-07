package com.cms.dto.audit;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ComplianceReportDto {
    private String id;
    private String reportType;
    private String status;
    private String dateFrom;
    private String dateTo;
    private Integer totalEvents;
    private Long fileSize;
    private String errorMessage;
    private String createdAt;
    private String completedAt;
}
