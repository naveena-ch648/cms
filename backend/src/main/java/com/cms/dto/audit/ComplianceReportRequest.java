package com.cms.dto.audit;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ComplianceReportRequest {
    @NotBlank
    private String reportType;
    @NotNull
    private String dateFrom;
    @NotNull
    private String dateTo;
}
