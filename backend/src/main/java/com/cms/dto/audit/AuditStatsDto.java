package com.cms.dto.audit;

import com.cms.entity.AuditCategory;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class AuditStatsDto {
    private long totalEvents;
    private Map<AuditCategory, Long> byCategory;
    private Map<String, Long> byOutcome;
}
