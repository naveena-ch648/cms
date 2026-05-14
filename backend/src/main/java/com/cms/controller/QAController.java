package com.cms.controller;

import com.cms.config.RateLimiter;
import com.cms.dto.ApiResponse;
import com.cms.dto.qa.*;
import com.cms.entity.Conversation;
import com.cms.entity.Conversation.ConversationStatus;
import com.cms.entity.ConversationMessage;
import com.cms.entity.EmbeddingJob;
import com.cms.entity.FileEntity;
import com.cms.entity.Workspace;
import com.cms.repository.WorkspaceRepository;
import com.cms.security.UserPrincipal;
import com.cms.service.ConversationService;
import com.cms.service.EmbeddingJobService;
import com.cms.service.FileService;
import com.cms.service.QAService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.springframework.lang.Nullable;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/v1/qa")
public class QAController {

    private final QAService qaService;
    private final ConversationService conversationService;
    private final EmbeddingJobService embeddingJobService;
    private final WorkspaceRepository workspaceRepository;
    private final FileService fileService;
    private final EmbeddingModel embeddingModel;
    private final ObjectMapper objectMapper;
    private final RateLimiter rateLimiter;

    public QAController(QAService qaService,
                        ConversationService conversationService,
                        EmbeddingJobService embeddingJobService,
                        WorkspaceRepository workspaceRepository,
                        FileService fileService,
                        @Nullable EmbeddingModel embeddingModel,
                        ObjectMapper objectMapper,
                        RateLimiter rateLimiter) {
        this.qaService = qaService;
        this.conversationService = conversationService;
        this.embeddingJobService = embeddingJobService;
        this.workspaceRepository = workspaceRepository;
        this.fileService = fileService;
        this.embeddingModel = embeddingModel;
        this.objectMapper = objectMapper;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/ask")
    public ResponseEntity<ApiResponse<AskResponse>> ask(
            @Valid @RequestBody AskRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        // Rate limit: 10 ask requests per minute per user
        if (rateLimiter.isRateLimited("ask:" + principal.getId(), 10, java.time.Duration.ofMinutes(1))) {
            return ResponseEntity.status(429).body(ApiResponse.ok(null));
        }

        Workspace workspace = workspaceRepository.findByUuid(request.getWorkspaceId())
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found"));

        if (!workspace.getOrganization().getId().equals(principal.getOrganizationId())) {
            return ResponseEntity.status(403).body(ApiResponse.ok(null));
        }

        List<String> accessibleDocIds = fileService.getAccessibleFileUuids(
                principal.getId(), workspace.getId());

        List<Float> questionEmbedding = generateEmbedding(request.getQuestion());

        AskResponse response = qaService.ask(request, principal.getId(),
                principal.getOrganizationId(), accessibleDocIds, workspace.getId(), questionEmbedding);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    private List<Float> generateEmbedding(String text) {
        if (embeddingModel == null) {
            return List.of();
        }
        float[] vector = embeddingModel.embed(TextSegment.from(text)).content().vector();
        List<Float> result = new java.util.ArrayList<>(vector.length);
        for (float v : vector) {
            result.add(v);
        }
        return result;
    }

    @PostMapping("/summarize")
    public ResponseEntity<ApiResponse<SummarizeResponse>> summarize(
            @Valid @RequestBody SummarizeRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        // Rate limit: 5 summarize requests per minute per user
        if (rateLimiter.isRateLimited("summarize:" + principal.getId(), 5, java.time.Duration.ofMinutes(1))) {
            return ResponseEntity.status(429).body(ApiResponse.ok(null));
        }

        Workspace workspace = workspaceRepository.findByUuid(request.getWorkspaceId())
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found"));

        if (!workspace.getOrganization().getId().equals(principal.getOrganizationId())) {
            return ResponseEntity.status(403).body(ApiResponse.ok(null));
        }

        // Generate embedding for summarization query
        List<Float> summaryEmbedding = generateEmbedding("summarize the main points of this document");

        SummarizeResponse response = qaService.summarize(request, request.getDocumentId(),
                workspace.getId(), summaryEmbedding);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<Page<ConversationDto>>> listConversations(
            @RequestParam String workspaceId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {

        Workspace workspace = workspaceRepository.findByUuid(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found"));

        if (!workspace.getOrganization().getId().equals(principal.getOrganizationId())) {
            return ResponseEntity.status(403).body(ApiResponse.ok(null));
        }

        ConversationStatus statusEnum = status != null ? ConversationStatus.valueOf(status.toUpperCase()) : null;
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));

        Page<Conversation> conversations = conversationService.listConversations(
                principal.getId(), workspace.getId(), statusEnum, search, pageable);

        Page<ConversationDto> dtos = conversations.map(c -> ConversationDto.builder()
                .id(c.getUuid())
                .title(c.getTitle())
                .status(c.getStatus().name())
                .messageCount(c.getMessageCount())
                .createdAt(c.getCreatedAt() != null ? c.getCreatedAt().toString() : null)
                .updatedAt(c.getUpdatedAt() != null ? c.getUpdatedAt().toString() : null)
                .build());

        return ResponseEntity.ok(ApiResponse.ok(dtos));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<ApiResponse<Page<MessageDto>>> getMessages(
            @PathVariable String conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal UserPrincipal principal) {

        Conversation conversation = conversationService.getConversationByUuid(conversationId);

        Page<ConversationMessage> messages = conversationService.getMessages(
                conversation.getId(), PageRequest.of(page, size));

        Page<MessageDto> dtos = messages.map(m -> MessageDto.builder()
                .id(m.getUuid())
                .role(m.getRole().name())
                .content(m.getContent())
                .citations(parseCitations(m.getCitations()))
                .modelUsed(m.getModelUsed())
                .tokenCount(m.getTokenCount() != null ? m.getTokenCount() : 0)
                .createdAt(m.getCreatedAt() != null ? m.getCreatedAt().toString() : null)
                .build());

        return ResponseEntity.ok(ApiResponse.ok(dtos));
    }

    @DeleteMapping("/conversations/{conversationId}")
    public ResponseEntity<ApiResponse<Void>> deleteConversation(
            @PathVariable String conversationId,
            @RequestParam(defaultValue = "false") boolean permanent,
            @AuthenticationPrincipal UserPrincipal principal) {

        conversationService.deleteConversation(conversationId, permanent);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/embedding-status/{fileId}")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> getEmbeddingStatus(
            @PathVariable String fileId,
            @AuthenticationPrincipal UserPrincipal principal) {

        EmbeddingJob job = embeddingJobService.getLatestJob(Long.valueOf(fileId)).orElse(null);
        java.util.Map<String, Object> status = new java.util.HashMap<>();

        if (job == null) {
            status.put("status", "NOT_INDEXED");
            status.put("indexed", false);
        } else {
            status.put("status", job.getStatus().name());
            status.put("indexed", job.getStatus() == EmbeddingJob.JobStatus.COMPLETED);
            status.put("chunkCount", job.getChunkCount());
            status.put("embeddingModel", job.getEmbeddingModel());
            status.put("lastUpdated", job.getUpdatedAt() != null ? job.getUpdatedAt().toString() : null);
            if (job.getErrorMessage() != null) {
                status.put("error", job.getErrorMessage());
            }
        }

        return ResponseEntity.ok(ApiResponse.ok(status));
    }

    @PostMapping("/reindex/{fileId}")
    public ResponseEntity<ApiResponse<java.util.Map<String, String>>> reindexFile(
            @PathVariable String fileId,
            @AuthenticationPrincipal UserPrincipal principal) {

        FileEntity file = fileService.getByUuid(fileId);
        embeddingJobService.retriggerEmbedding(file);

        return ResponseEntity.ok(ApiResponse.ok(java.util.Map.of(
                "message", "Re-indexing triggered",
                "fileId", fileId
        )));
    }

    private List<CitationDto> parseCitations(String citationsJson) {
        if (citationsJson == null || citationsJson.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(citationsJson, new TypeReference<List<CitationDto>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
