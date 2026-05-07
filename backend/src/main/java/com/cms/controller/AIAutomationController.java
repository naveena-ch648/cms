package com.cms.controller;

import com.cms.dto.ApiResponse;
import com.cms.dto.ai.*;
import com.cms.security.UserPrincipal;
import com.cms.service.AIAutomationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AIAutomationController {

    private final AIAutomationService aiAutomationService;

    @GetMapping("/files/{fileId}/suggestions")
    public ResponseEntity<ApiResponse<AISuggestionsResponse>> getSuggestions(
            @PathVariable String fileId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AISuggestionsResponse response = aiAutomationService.getSuggestions(fileId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/files/{fileId}/accept-tags")
    public ResponseEntity<ApiResponse<Map<String, Object>>> acceptTags(
            @PathVariable String fileId,
            @Valid @RequestBody AcceptTagsRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        Map<String, Object> result = aiAutomationService.acceptTags(fileId, request, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping("/files/{fileId}/accept-classification")
    public ResponseEntity<ApiResponse<Map<String, Object>>> acceptClassification(
            @PathVariable String fileId,
            @Valid @RequestBody AcceptClassificationRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        Map<String, Object> result = aiAutomationService.acceptClassification(fileId, request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping("/files/{fileId}/regenerate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> regenerate(
            @PathVariable String fileId,
            @RequestBody(required = false) RegenerateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (request == null) request = new RegenerateRequest();
        Map<String, Object> result = aiAutomationService.regenerate(fileId, request, principal.getId());
        return ResponseEntity.status(202).body(ApiResponse.ok(result));
    }

    @GetMapping("/files/{fileId}/jobs")
    public ResponseEntity<ApiResponse<Page<AIJobResponse>>> getJobs(
            @PathVariable String fileId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        Page<AIJobResponse> jobs = aiAutomationService.getJobHistory(fileId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.ok(jobs));
    }

    @PostMapping("/files/{fileId}/apply-workflow")
    public ResponseEntity<ApiResponse<Map<String, Object>>> applyWorkflow(
            @PathVariable String fileId,
            @Valid @RequestBody ApplyWorkflowRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        Map<String, Object> result = aiAutomationService.applyWorkflow(fileId, request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/config")
    public ResponseEntity<ApiResponse<AIConfigResponse>> getConfig(
            @AuthenticationPrincipal UserPrincipal principal) {
        AIConfigResponse config = aiAutomationService.getConfig(principal.getOrganizationId());
        return ResponseEntity.ok(ApiResponse.ok(config));
    }

    @PutMapping("/config")
    public ResponseEntity<ApiResponse<AIConfigResponse>> updateConfig(
            @Valid @RequestBody AIConfigRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AIConfigResponse config = aiAutomationService.updateConfig(principal.getOrganizationId(), request);
        return ResponseEntity.ok(ApiResponse.ok(config));
    }
}
