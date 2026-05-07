package com.cms.dto.ai;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIConfigResponse {
    private List<String> enabledFeatures;
    private Integer confidenceThreshold;
    private AIConfigRequest.SensitivityPatterns sensitivityPatterns;
    private Map<String, String> workflowMappings;
}
