package com.cms.dto.audit;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AuditSearchResponse {
    private List<AuditEventDto> events;
    private long total;
    private int page;
    private int size;
}
