package com.cms.dto.ai;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIConfigRequest {
    private List<String> enabledFeatures;
    private Integer confidenceThreshold;
    private SensitivityPatterns sensitivityPatterns;
    private Map<String, String> workflowMappings;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SensitivityPatterns {
        private List<CustomPattern> customPatterns;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CustomPattern {
        private String name;
        private String pattern;
    }
}
