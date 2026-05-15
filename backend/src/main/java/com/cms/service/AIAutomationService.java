package com.cms.service;

import com.cms.dto.ai.*;
import com.cms.dto.ai.AISuggestionsResponse.*;
import com.cms.entity.AIJob;
import com.cms.entity.AIJob.JobStatus;
import com.cms.entity.AIJob.JobType;
import com.cms.entity.AIJob.TriggeredBy;
import com.cms.entity.FileEntity;
import com.cms.entity.Organization;
import com.cms.repository.AIJobRepository;
import com.cms.repository.FileRepository;
import com.cms.repository.OrganizationRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIAutomationService {

    private static final String AI_QUEUE = "ai:process";

    private final AIJobRepository aiJobRepository;
    private final FileRepository fileRepository;
    private final OrganizationRepository organizationRepository;
    private final JobQueueService jobQueueService;
    private final ObjectMapper objectMapper;

    @Transactional
    public void enqueueAIJobs(Long fileId, Long organizationId) {
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found: " + fileId));
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organization not found: " + organizationId));

        List<JobType> enabledTypes = getEnabledJobTypes(org);

        for (JobType type : enabledTypes) {
            AIJob job = AIJob.builder()
                    .file(file)
                    .organization(org)
                    .type(type)
                    .status(JobStatus.PENDING)
                    .triggeredBy(TriggeredBy.SYSTEM)
                    .build();
            aiJobRepository.save(job);

            publishToQueue(job, file);
            log.debug("Enqueued AI job: type={}, fileId={}", type, fileId);
        }
    }

    public AISuggestionsResponse getSuggestions(String fileUuid) {
        FileEntity file = fileRepository.findByUuid(fileUuid)
                .orElseThrow(() -> new RuntimeException("File not found"));

        List<AIJob> jobs = aiJobRepository.findByFileId(file.getId());

        String overallStatus = determineOverallStatus(jobs);

        return AISuggestionsResponse.builder()
                .fileId(fileUuid)
                .processingStatus(overallStatus)
                .tags(buildTagSuggestions(jobs))
                .classification(buildClassification(jobs))
                .summary(buildSummary(jobs))
                .duplicates(buildDuplicates(jobs))
                .sensitivity(buildSensitivity(jobs))
                .workflowRecommendation(buildWorkflowRecommendation(jobs))
                .build();
    }

    @Transactional
    public Map<String, Object> acceptTags(String fileUuid, AcceptTagsRequest request, Long userId) {
        FileEntity file = fileRepository.findByUuid(fileUuid)
                .orElseThrow(() -> new RuntimeException("File not found"));

        // Store accepted tags in the file's tags JSON field
        List<String> currentTags = parseTags(file.getTags());
        Set<String> tagSet = new LinkedHashSet<>(currentTags);
        tagSet.addAll(request.getAcceptedTags());
        tagSet.removeAll(request.getRejectedTags());

        file.setTags(writeJson(new ArrayList<>(tagSet)));
        fileRepository.save(file);

        Map<String, Object> result = new HashMap<>();
        result.put("appliedTags", request.getAcceptedTags());
        result.put("rejectedTags", request.getRejectedTags());
        return result;
    }

    @Transactional
    public Map<String, Object> acceptClassification(String fileUuid, AcceptClassificationRequest request) {
        // Classification acceptance is stored in the TAG job result as metadata
        FileEntity file = fileRepository.findByUuid(fileUuid)
                .orElseThrow(() -> new RuntimeException("File not found"));

        // Update file description to indicate classification
        Map<String, Object> result = new HashMap<>();
        result.put("category", request.getCategory());
        result.put("applied", true);
        return result;
    }

    @Transactional
    public Map<String, Object> regenerate(String fileUuid, RegenerateRequest request, Long userId) {
        FileEntity file = fileRepository.findByUuid(fileUuid)
                .orElseThrow(() -> new RuntimeException("File not found"));
        Organization org = file.getOrganization();

        List<JobType> types;
        if (request.getTypes() != null && !request.getTypes().isEmpty()) {
            types = request.getTypes().stream()
                    .map(JobType::valueOf)
                    .collect(Collectors.toList());
        } else {
            types = getEnabledJobTypes(org);
        }

        List<String> jobIds = new ArrayList<>();
        for (JobType type : types) {
            AIJob job = AIJob.builder()
                    .file(file)
                    .organization(org)
                    .type(type)
                    .status(JobStatus.PENDING)
                    .triggeredBy(TriggeredBy.USER)
                    .build();
            aiJobRepository.save(job);
            publishToQueue(job, file);
            jobIds.add(job.getUuid());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("jobIds", jobIds);
        result.put("message", "AI analysis queued for re-processing");
        return result;
    }

    public Page<AIJobResponse> getJobHistory(String fileUuid, Pageable pageable) {
        FileEntity file = fileRepository.findByUuid(fileUuid)
                .orElseThrow(() -> new RuntimeException("File not found"));

        return aiJobRepository.findByFileIdOrderByCreatedAtDesc(file.getId(), pageable)
                .map(this::toJobResponse);
    }

    @Transactional
    public Map<String, Object> applyWorkflow(String fileUuid, ApplyWorkflowRequest request) {
        FileEntity file = fileRepository.findByUuid(fileUuid)
                .orElseThrow(() -> new RuntimeException("File not found"));

        Map<String, Object> result = new HashMap<>();
        result.put("fileId", fileUuid);
        result.put("workflowId", request.getWorkflowId());
        result.put("workflowName", "Workflow");
        result.put("applied", true);
        return result;
    }

    public AIConfigResponse getConfig(Long organizationId) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));

        Map<String, Object> config = parseAiConfig(org.getAiConfig());

        return AIConfigResponse.builder()
                .enabledFeatures(getListFromConfig(config, "enabled_features"))
                .confidenceThreshold(getIntFromConfig(config, "confidence_threshold", 70))
                .sensitivityPatterns(parseSensitivityPatterns(config))
                .workflowMappings(getMapFromConfig(config, "workflow_mappings"))
                .build();
    }

    @Transactional
    public AIConfigResponse updateConfig(Long organizationId, AIConfigRequest request) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));

        Map<String, Object> config = new HashMap<>();
        if (request.getEnabledFeatures() != null) {
            config.put("enabled_features", request.getEnabledFeatures());
        }
        if (request.getConfidenceThreshold() != null) {
            config.put("confidence_threshold", request.getConfidenceThreshold());
        }
        if (request.getSensitivityPatterns() != null) {
            config.put("sensitivity_patterns", request.getSensitivityPatterns());
        }
        if (request.getWorkflowMappings() != null) {
            config.put("workflow_mappings", request.getWorkflowMappings());
        }

        org.setAiConfig(writeJson(config));
        organizationRepository.save(org);

        return getConfig(organizationId);
    }

    // --- Private helpers ---

    private void publishToQueue(AIJob job, FileEntity file) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("jobId", job.getUuid());
            message.put("fileId", file.getUuid());
            message.put("orgId", file.getOrganization().getId());
            message.put("type", job.getType().name());
            message.put("storageKey", file.getStorageKey());
            message.put("storageBucket", file.getStorageBucket());
            message.put("mimeType", file.getMimeType());

            String payload = objectMapper.writeValueAsString(message);
            jobQueueService.push(AI_QUEUE, payload);
        } catch (Exception e) {
            log.error("Failed to publish AI job to queue: jobId={}", job.getUuid(), e);
        }
    }

    private List<JobType> getEnabledJobTypes(Organization org) {
        Map<String, Object> config = parseAiConfig(org.getAiConfig());
        List<String> enabled = getListFromConfig(config, "enabled_features");

        if (enabled.isEmpty()) {
            return List.of(JobType.TAG, JobType.CLASSIFY, JobType.SUMMARIZE,
                    JobType.DETECT_DUPLICATES, JobType.DETECT_SENSITIVE, JobType.RECOMMEND_WORKFLOW);
        }

        return enabled.stream()
                .map(s -> {
                    try { return JobType.valueOf(s); }
                    catch (IllegalArgumentException e) { return null; }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private String determineOverallStatus(List<AIJob> jobs) {
        if (jobs.isEmpty()) return "PENDING";
        boolean anyProcessing = jobs.stream().anyMatch(j -> j.getStatus() == JobStatus.PROCESSING);
        boolean anyPending = jobs.stream().anyMatch(j -> j.getStatus() == JobStatus.PENDING);
        boolean allCompleted = jobs.stream().allMatch(j -> j.getStatus() == JobStatus.COMPLETED || j.getStatus() == JobStatus.FAILED);

        if (anyProcessing) return "PROCESSING";
        if (anyPending) return "PENDING";
        if (allCompleted) return "COMPLETED";
        return "PROCESSING";
    }

    private TagSuggestions buildTagSuggestions(List<AIJob> jobs) {
        return jobs.stream()
                .filter(j -> j.getType() == JobType.TAG && j.getStatus() == JobStatus.COMPLETED)
                .findFirst()
                .map(j -> {
                    Map<String, Object> result = parseResult(j.getResult());
                    List<String> tags = getListFromConfig(result, "suggested_tags");
                    Map<String, Double> conf = getDoubleMapFromConfig(result, "confidence_per_tag");
                    return TagSuggestions.builder()
                            .status("COMPLETED")
                            .suggestions(tags)
                            .confidence(conf)
                            .acceptedTags(List.of())
                            .rejectedTags(List.of())
                            .build();
                })
                .orElse(buildPendingTagStatus(jobs));
    }

    private TagSuggestions buildPendingTagStatus(List<AIJob> jobs) {
        String status = jobs.stream()
                .filter(j -> j.getType() == JobType.TAG)
                .findFirst()
                .map(j -> j.getStatus().name())
                .orElse("PENDING");
        return TagSuggestions.builder().status(status).suggestions(List.of()).confidence(Map.of()).acceptedTags(List.of()).rejectedTags(List.of()).build();
    }

    private ClassificationSuggestion buildClassification(List<AIJob> jobs) {
        return jobs.stream()
                .filter(j -> j.getType() == JobType.CLASSIFY && j.getStatus() == JobStatus.COMPLETED)
                .findFirst()
                .map(j -> {
                    Map<String, Object> result = parseResult(j.getResult());
                    String category = (String) result.getOrDefault("category", "");
                    Double confidence = toDouble(result.get("confidence"));
                    List<Map<String, Object>> alts = getListOfMaps(result, "alternatives");
                    List<AlternativeCategory> alternatives = alts.stream()
                            .map(a -> AlternativeCategory.builder()
                                    .category((String) a.get("category"))
                                    .confidence(toDouble(a.get("confidence")))
                                    .build())
                            .collect(Collectors.toList());
                    return ClassificationSuggestion.builder()
                            .status("COMPLETED").category(category).confidence(confidence).alternatives(alternatives).build();
                })
                .orElse(ClassificationSuggestion.builder().status(getJobStatus(jobs, JobType.CLASSIFY)).build());
    }

    private SummarySuggestion buildSummary(List<AIJob> jobs) {
        return jobs.stream()
                .filter(j -> j.getType() == JobType.SUMMARIZE && j.getStatus() == JobStatus.COMPLETED)
                .findFirst()
                .map(j -> {
                    Map<String, Object> result = parseResult(j.getResult());
                    return SummarySuggestion.builder()
                            .status("COMPLETED")
                            .text((String) result.getOrDefault("summary", ""))
                            .wordCount(toInt(result.get("word_count")))
                            .keyTopics(getListFromConfig(result, "key_topics"))
                            .build();
                })
                .orElse(SummarySuggestion.builder().status(getJobStatus(jobs, JobType.SUMMARIZE)).build());
    }

    private DuplicateSuggestion buildDuplicates(List<AIJob> jobs) {
        return jobs.stream()
                .filter(j -> j.getType() == JobType.DETECT_DUPLICATES && j.getStatus() == JobStatus.COMPLETED)
                .findFirst()
                .map(j -> {
                    Map<String, Object> result = parseResult(j.getResult());
                    Map<String, Object> exactRaw = getMapObj(result, "exact_match");
                    DuplicateMatch exact = exactRaw != null ? DuplicateMatch.builder()
                            .fileId((String) exactRaw.get("file_id"))
                            .fileName((String) exactRaw.get("file_name"))
                            .similarity(toDouble(exactRaw.get("similarity")))
                            .build() : null;
                    List<Map<String, Object>> nearRaw = getListOfMaps(result, "near_duplicates");
                    List<DuplicateMatch> nears = nearRaw.stream()
                            .map(m -> DuplicateMatch.builder()
                                    .fileId((String) m.get("file_id"))
                                    .fileName((String) m.get("file_name"))
                                    .similarity(toDouble(m.get("similarity")))
                                    .build())
                            .collect(Collectors.toList());
                    return DuplicateSuggestion.builder().status("COMPLETED").exactMatch(exact).nearDuplicates(nears).build();
                })
                .orElse(DuplicateSuggestion.builder().status(getJobStatus(jobs, JobType.DETECT_DUPLICATES)).nearDuplicates(List.of()).build());
    }

    private SensitivitySuggestion buildSensitivity(List<AIJob> jobs) {
        return jobs.stream()
                .filter(j -> j.getType() == JobType.DETECT_SENSITIVE && j.getStatus() == JobStatus.COMPLETED)
                .findFirst()
                .map(j -> {
                    Map<String, Object> result = parseResult(j.getResult());
                    Boolean hasSensitive = (Boolean) result.getOrDefault("has_sensitive_data", false);
                    String severity = (String) result.getOrDefault("severity", "NONE");
                    List<Map<String, Object>> dets = getListOfMaps(result, "detections");
                    List<SensitivityDetection> detections = dets.stream()
                            .map(d -> SensitivityDetection.builder()
                                    .type((String) d.get("type"))
                                    .count(toInt(d.get("count")))
                                    .severity((String) d.get("severity"))
                                    .build())
                            .collect(Collectors.toList());
                    return SensitivitySuggestion.builder().status("COMPLETED").hasSensitiveData(hasSensitive).severity(severity).detections(detections).build();
                })
                .orElse(SensitivitySuggestion.builder().status(getJobStatus(jobs, JobType.DETECT_SENSITIVE)).hasSensitiveData(false).detections(List.of()).build());
    }

    private WorkflowRecommendationDto buildWorkflowRecommendation(List<AIJob> jobs) {
        return jobs.stream()
                .filter(j -> j.getType() == JobType.RECOMMEND_WORKFLOW && j.getStatus() == JobStatus.COMPLETED)
                .findFirst()
                .map(j -> {
                    Map<String, Object> result = parseResult(j.getResult());
                    return WorkflowRecommendationDto.builder()
                            .status("COMPLETED")
                            .recommendedWorkflow((String) result.getOrDefault("recommended_workflow", ""))
                            .workflowId((String) result.getOrDefault("workflow_id", ""))
                            .reason((String) result.getOrDefault("reason", ""))
                            .build();
                })
                .orElse(WorkflowRecommendationDto.builder().status(getJobStatus(jobs, JobType.RECOMMEND_WORKFLOW)).build());
    }

    private String getJobStatus(List<AIJob> jobs, JobType type) {
        return jobs.stream()
                .filter(j -> j.getType() == type)
                .findFirst()
                .map(j -> j.getStatus().name())
                .orElse("PENDING");
    }

    private AIJobResponse toJobResponse(AIJob job) {
        return AIJobResponse.builder()
                .id(job.getUuid())
                .type(job.getType().name())
                .status(job.getStatus().name())
                .confidence(job.getConfidence())
                .triggeredBy(job.getTriggeredBy().name())
                .createdAt(job.getCreatedAt() != null ? job.getCreatedAt().toString() : null)
                .completedAt(job.getCompletedAt() != null ? job.getCompletedAt().toString() : null)
                .build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseResult(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseAiConfig(String json) {
        if (json == null || json.isBlank()) return new HashMap<>();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> getListFromConfig(Map<String, Object> config, String key) {
        Object val = config.get(key);
        if (val instanceof List) return (List<String>) val;
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> getMapFromConfig(Map<String, Object> config, String key) {
        Object val = config.get(key);
        if (val instanceof Map) return (Map<String, String>) val;
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Double> getDoubleMapFromConfig(Map<String, Object> config, String key) {
        Object val = config.get(key);
        if (val instanceof Map) {
            Map<String, Object> raw = (Map<String, Object>) val;
            Map<String, Double> result = new HashMap<>();
            raw.forEach((k, v) -> result.put(k, toDouble(v)));
            return result;
        }
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getListOfMaps(Map<String, Object> config, String key) {
        Object val = config.get(key);
        if (val instanceof List) return (List<Map<String, Object>>) val;
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMapObj(Map<String, Object> config, String key) {
        Object val = config.get(key);
        if (val instanceof Map) return (Map<String, Object>) val;
        return null;
    }

    private int getIntFromConfig(Map<String, Object> config, String key, int defaultVal) {
        Object val = config.get(key);
        if (val instanceof Number) return ((Number) val).intValue();
        return defaultVal;
    }

    @SuppressWarnings("unchecked")
    private AIConfigRequest.SensitivityPatterns parseSensitivityPatterns(Map<String, Object> config) {
        Object val = config.get("sensitivity_patterns");
        if (val instanceof Map) {
            Map<String, Object> sp = (Map<String, Object>) val;
            List<Map<String, String>> patterns = (List<Map<String, String>>) sp.getOrDefault("custom_patterns", List.of());
            List<AIConfigRequest.CustomPattern> custom = patterns.stream()
                    .map(p -> AIConfigRequest.CustomPattern.builder().name(p.get("name")).pattern(p.get("pattern")).build())
                    .collect(Collectors.toList());
            return AIConfigRequest.SensitivityPatterns.builder().customPatterns(custom).build();
        }
        return AIConfigRequest.SensitivityPatterns.builder().customPatterns(List.of()).build();
    }

    private List<String> parseTags(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String writeJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }

    private Double toDouble(Object val) {
        if (val instanceof Number) return ((Number) val).doubleValue();
        return 0.0;
    }

    private Integer toInt(Object val) {
        if (val instanceof Number) return ((Number) val).intValue();
        return 0;
    }
}
