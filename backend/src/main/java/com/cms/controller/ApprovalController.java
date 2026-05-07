package com.cms.controller;

import com.cms.dto.ApiResponse;
import com.cms.dto.workflow.ApprovalDecisionRequest;
import com.cms.dto.workflow.ApprovalDecisionResponse;
import com.cms.dto.workflow.ApprovalRequestResponse;
import com.cms.dto.workflow.ApprovalSubmitRequest;
import com.cms.entity.ApprovalRequest;
import com.cms.security.UserPrincipal;
import com.cms.service.ApprovalService;
import com.cms.service.WorkspaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalService approvalService;
    private final WorkspaceService workspaceService;

    @PostMapping("/files/{fileId}/approvals")
    public ResponseEntity<ApiResponse<ApprovalRequestResponse>> submitForApproval(
            @PathVariable String fileId,
            @Valid @RequestBody ApprovalSubmitRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        ApprovalRequestResponse response = approvalService.submitForApproval(
                fileId, request.getReviewerIds(), request.getComment(), principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/workspaces/{workspaceId}/approvals")
    public ResponseEntity<ApiResponse<List<ApprovalRequestResponse>>> listWorkspaceApprovals(
            @PathVariable String workspaceId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {

        Long wsId = workspaceService.getByUuid(workspaceId).getId();
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));

        ApprovalRequest.Status statusFilter = null;
        if (status != null && !status.isEmpty()) {
            statusFilter = ApprovalRequest.Status.valueOf(status.toUpperCase());
        }

        Page<ApprovalRequestResponse> result = approvalService.listWorkspaceApprovals(wsId, statusFilter, pageable);

        ApiResponse.PagedMeta pagedMeta = ApiResponse.PagedMeta.builder()
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();

        return ResponseEntity.ok(ApiResponse.ok(result.getContent(), pagedMeta));
    }

    @GetMapping("/approvals/{approvalId}")
    public ResponseEntity<ApiResponse<ApprovalRequestResponse>> getApproval(
            @PathVariable String approvalId,
            @AuthenticationPrincipal UserPrincipal principal) {

        ApprovalRequestResponse response = approvalService.getApproval(approvalId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/approvals/{approvalId}/decisions")
    public ResponseEntity<ApiResponse<ApprovalDecisionResponse>> decide(
            @PathVariable String approvalId,
            @Valid @RequestBody ApprovalDecisionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        ApprovalDecisionResponse response = approvalService.decide(
                approvalId, request.getDecision(), request.getComment(), principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/approvals/{approvalId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancel(
            @PathVariable String approvalId,
            @AuthenticationPrincipal UserPrincipal principal) {

        approvalService.cancel(approvalId, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/approvals/pending")
    public ResponseEntity<ApiResponse<List<ApprovalRequestResponse>>> listPendingForMe(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {

        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<ApprovalRequestResponse> result = approvalService.listPendingForReviewer(principal.getId(), pageable);

        ApiResponse.PagedMeta pagedMeta = ApiResponse.PagedMeta.builder()
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();

        return ResponseEntity.ok(ApiResponse.ok(result.getContent(), pagedMeta));
    }
}
