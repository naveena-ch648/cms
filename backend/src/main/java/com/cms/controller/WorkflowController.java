package com.cms.controller;

import com.cms.annotation.Audited;
import com.cms.dto.ApiResponse;
import com.cms.entity.AuditCategory;
import com.cms.entity.AuditEventType;
import com.cms.dto.workflow.BulkTransitionRequest;
import com.cms.dto.workflow.WorkflowStateResponse;
import com.cms.dto.workflow.WorkflowTransitionRequest;
import com.cms.dto.workflow.WorkflowTransitionResponse;
import com.cms.security.UserPrincipal;
import com.cms.service.WorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;

    @PostMapping("/files/{fileId}/workflow/transition")
    @Audited(event = AuditEventType.STATE_CHANGED, category = AuditCategory.WORKFLOW, resourceType = "file")
    public ResponseEntity<ApiResponse<WorkflowTransitionResponse>> transition(
            @PathVariable String fileId,
            @Valid @RequestBody WorkflowTransitionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        WorkflowTransitionResponse response = workflowService.transition(
                fileId, request.getTargetState(), request.getComment(), principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/files/bulk-workflow/transition")
    public ResponseEntity<ApiResponse<Map<String, Object>>> bulkTransition(
            @Valid @RequestBody BulkTransitionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        int count = workflowService.bulkTransition(
                request.getFileIds(), request.getTargetState(), request.getComment(), principal.getId());

        Map<String, Object> result = Map.of(
                "transitioned", count,
                "fileIds", request.getFileIds()
        );
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/files/{fileId}/workflow/history")
    public ResponseEntity<ApiResponse<List<WorkflowTransitionResponse>>> getHistory(
            @PathVariable String fileId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal UserPrincipal principal) {

        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<WorkflowTransitionResponse> result = workflowService.getHistory(fileId, pageable);

        ApiResponse.PagedMeta pagedMeta = ApiResponse.PagedMeta.builder()
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();

        return ResponseEntity.ok(ApiResponse.ok(result.getContent(), pagedMeta));
    }

    @GetMapping("/files/{fileId}/workflow/state")
    public ResponseEntity<ApiResponse<WorkflowStateResponse>> getState(
            @PathVariable String fileId,
            @AuthenticationPrincipal UserPrincipal principal) {

        WorkflowStateResponse response = workflowService.getState(fileId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
