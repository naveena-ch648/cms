package com.cms.dto.ai;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIJobResponse {
    private String id;
    private String type;
    private String status;
    private BigDecimal confidence;
    private String triggeredBy;
    private String createdAt;
    private String completedAt;
}
