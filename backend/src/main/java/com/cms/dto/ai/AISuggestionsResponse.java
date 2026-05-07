package com.cms.dto.ai;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AISuggestionsResponse {
    private String fileId;
    private String processingStatus;
    private TagSuggestions tags;
    private ClassificationSuggestion classification;
    private SummarySuggestion summary;
    private DuplicateSuggestion duplicates;
    private SensitivitySuggestion sensitivity;
    private WorkflowRecommendationDto workflowRecommendation;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TagSuggestions {
        private String status;
        private List<String> suggestions;
        private Map<String, Double> confidence;
        private List<String> acceptedTags;
        private List<String> rejectedTags;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ClassificationSuggestion {
        private String status;
        private String category;
        private Double confidence;
        private List<AlternativeCategory> alternatives;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AlternativeCategory {
        private String category;
        private Double confidence;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SummarySuggestion {
        private String status;
        private String text;
        private Integer wordCount;
        private List<String> keyTopics;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DuplicateMatch {
        private String fileId;
        private String fileName;
        private Double similarity;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DuplicateSuggestion {
        private String status;
        private DuplicateMatch exactMatch;
        private List<DuplicateMatch> nearDuplicates;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SensitivityDetection {
        private String type;
        private Integer count;
        private String severity;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SensitivitySuggestion {
        private String status;
        private Boolean hasSensitiveData;
        private String severity;
        private List<SensitivityDetection> detections;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WorkflowRecommendationDto {
        private String status;
        private String recommendedWorkflow;
        private String workflowId;
        private String reason;
    }
}
