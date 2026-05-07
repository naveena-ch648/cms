package com.cms.service;

import com.cms.dto.workflow.ApprovalDecisionResponse;
import com.cms.dto.workflow.ApprovalRequestResponse;
import com.cms.entity.*;
import com.cms.exception.BusinessRuleException;
import com.cms.exception.ResourceNotFoundException;
import com.cms.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApprovalService {

    private final ApprovalRequestRepository approvalRequestRepository;
    private final ApprovalDecisionRepository approvalDecisionRepository;
    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final WorkflowService workflowService;
    private final NotificationService notificationService;
    private final ActivityEventService activityEventService;

    @Transactional
    public ApprovalRequestResponse submitForApproval(String fileUuid, List<String> reviewerUuids, String comment, Long submitterId) {
        FileEntity file = fileRepository.findByUuid(fileUuid)
                .orElseThrow(() -> new ResourceNotFoundException("File not found: " + fileUuid));

        User submitter = userRepository.findById(submitterId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Verify file is in REVIEW state (must transition to REVIEW first)
        if (file.getWorkflowState() != WorkflowState.REVIEW) {
            throw new BusinessRuleException("INVALID_STATE",
                    "File must be in REVIEW state to submit for approval. Current state: " + file.getWorkflowState());
        }

        // Check no pending approval already exists
        var existing = approvalRequestRepository.findByFileIdAndStatus(file.getId(), ApprovalRequest.Status.PENDING);
        if (existing.isPresent()) {
            throw new BusinessRuleException("APPROVAL_EXISTS",
                    "An active approval request already exists for this file");
        }

        // Resolve reviewers
        List<User> reviewers = reviewerUuids.stream()
                .map(uuid -> userRepository.findByUuid(uuid)
                        .orElseThrow(() -> new ResourceNotFoundException("Reviewer not found: " + uuid)))
                .collect(Collectors.toList());

        if (reviewers.isEmpty()) {
            throw new BusinessRuleException("NO_REVIEWERS", "At least one reviewer is required");
        }

        // Create approval request
        ApprovalRequest request = ApprovalRequest.builder()
                .file(file)
                .submitter(submitter)
                .workspace(file.getWorkspace())
                .status(ApprovalRequest.Status.PENDING)
                .fromState(WorkflowState.REVIEW.name())
                .toState(WorkflowState.APPROVED.name())
                .comment(comment)
                .build();

        request = approvalRequestRepository.save(request);

        // Create decision records for each reviewer
        for (User reviewer : reviewers) {
            ApprovalDecision decision = ApprovalDecision.builder()
                    .approvalRequest(request)
                    .reviewer(reviewer)
                    .decision(ApprovalDecision.Decision.PENDING)
                    .build();
            approvalDecisionRepository.save(decision);

            // Send notification to reviewer
            notificationService.createNotification(
                    reviewer,
                    Notification.Type.APPROVAL_REQUESTED,
                    "Approval requested",
                    submitter.getFirstName() + " " + submitter.getLastName() + " requested your approval for \"" + file.getName() + "\"",
                    "APPROVAL",
                    request.getUuid(),
                    submitter
            );
        }

        log.info("Approval request created: file={} submitter={} reviewers={}", fileUuid, submitterId, reviewerUuids.size());

        // Record activity event
        activityEventService.recordEvent(submitter, ActivityEvent.ActionType.APPROVAL_SUBMITTED,
                "FILE", file.getUuid(), file.getName(),
                file.getWorkspace(), file.getWorkspace().getOrganization(), null);

        List<ApprovalDecision> decisions = approvalDecisionRepository.findByApprovalRequestId(request.getId());
        return ApprovalRequestResponse.from(request, decisions);
    }

    @Transactional
    public ApprovalDecisionResponse decide(String approvalUuid, String decisionStr, String comment, Long reviewerId) {
        ApprovalRequest request = approvalRequestRepository.findByUuid(approvalUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Approval request not found: " + approvalUuid));

        if (request.getStatus() != ApprovalRequest.Status.PENDING) {
            throw new BusinessRuleException("NOT_PENDING", "Approval request is no longer pending");
        }

        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ApprovalDecision decisionRecord = approvalDecisionRepository
                .findByApprovalRequestIdAndReviewerId(request.getId(), reviewerId)
                .orElseThrow(() -> new BusinessRuleException("NOT_REVIEWER", "You are not a reviewer for this approval request"));

        if (decisionRecord.getDecision() != ApprovalDecision.Decision.PENDING) {
            throw new BusinessRuleException("ALREADY_DECIDED", "You have already submitted your decision");
        }

        ApprovalDecision.Decision decision;
        try {
            decision = ApprovalDecision.Decision.valueOf(decisionStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("INVALID_DECISION", "Invalid decision: " + decisionStr + ". Use APPROVED or REJECTED");
        }

        if (decision == ApprovalDecision.Decision.PENDING) {
            throw new BusinessRuleException("INVALID_DECISION", "Cannot set decision to PENDING");
        }

        // Record the decision
        decisionRecord.setDecision(decision);
        decisionRecord.setComment(comment);
        decisionRecord.setDecidedAt(Instant.now());
        approvalDecisionRepository.save(decisionRecord);

        // Check if approval is complete
        List<ApprovalDecision> allDecisions = approvalDecisionRepository.findByApprovalRequestId(request.getId());
        long approvedCount = allDecisions.stream()
                .filter(d -> d.getDecision() == ApprovalDecision.Decision.APPROVED)
                .count();
        long rejectedCount = allDecisions.stream()
                .filter(d -> d.getDecision() == ApprovalDecision.Decision.REJECTED)
                .count();
        long totalReviewers = allDecisions.size();

        if (decision == ApprovalDecision.Decision.REJECTED) {
            // Any rejection → reject approval, move back to DRAFT
            request.setStatus(ApprovalRequest.Status.REJECTED);
            request.setCompletedAt(Instant.now());
            approvalRequestRepository.save(request);

            FileEntity file = request.getFile();
            workflowService.rejectToState(file, WorkflowState.DRAFT, reviewer,
                    "Rejected by " + reviewer.getFirstName() + " " + reviewer.getLastName() + ": " + (comment != null ? comment : ""));

            // Notify submitter
            notificationService.createNotification(
                    request.getSubmitter(),
                    Notification.Type.APPROVAL_REJECTED,
                    "Approval rejected",
                    reviewer.getFirstName() + " " + reviewer.getLastName() + " rejected \"" + file.getName() + "\"",
                    "APPROVAL",
                    request.getUuid(),
                    reviewer
            );

            log.info("Approval rejected: request={} reviewer={}", approvalUuid, reviewerId);
        } else if (approvedCount == totalReviewers) {
            // All approved → complete approval, transition to APPROVED
            request.setStatus(ApprovalRequest.Status.APPROVED);
            request.setCompletedAt(Instant.now());
            approvalRequestRepository.save(request);

            FileEntity file = request.getFile();
            workflowService.transitionViaApproval(file, WorkflowState.APPROVED, reviewer, request);

            // Notify submitter
            notificationService.createNotification(
                    request.getSubmitter(),
                    Notification.Type.APPROVAL_APPROVED,
                    "Approval granted",
                    "\"" + file.getName() + "\" has been approved by all reviewers",
                    "APPROVAL",
                    request.getUuid(),
                    reviewer
            );

            log.info("Approval completed: request={}", approvalUuid);
        }

        // Record activity event
        activityEventService.recordEvent(reviewer, ActivityEvent.ActionType.APPROVAL_DECIDED,
                "FILE", request.getFile().getUuid(), request.getFile().getName(),
                request.getWorkspace(), request.getWorkspace().getOrganization(), null);

        return ApprovalDecisionResponse.from(decisionRecord, request.getStatus().name(), approvedCount, totalReviewers);
    }

    @Transactional
    public void cancel(String approvalUuid, Long userId) {
        ApprovalRequest request = approvalRequestRepository.findByUuid(approvalUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Approval request not found: " + approvalUuid));

        if (request.getStatus() != ApprovalRequest.Status.PENDING) {
            throw new BusinessRuleException("NOT_PENDING", "Only pending approval requests can be cancelled");
        }

        if (!request.getSubmitter().getId().equals(userId)) {
            throw new BusinessRuleException("NOT_SUBMITTER", "Only the submitter can cancel an approval request");
        }

        request.setStatus(ApprovalRequest.Status.CANCELLED);
        request.setCompletedAt(Instant.now());
        approvalRequestRepository.save(request);

        log.info("Approval cancelled: request={} by={}", approvalUuid, userId);
    }

    @Transactional(readOnly = true)
    public ApprovalRequestResponse getApproval(String approvalUuid) {
        ApprovalRequest request = approvalRequestRepository.findByUuid(approvalUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Approval request not found: " + approvalUuid));

        List<ApprovalDecision> decisions = approvalDecisionRepository.findByApprovalRequestId(request.getId());
        return ApprovalRequestResponse.from(request, decisions);
    }

    @Transactional(readOnly = true)
    public Page<ApprovalRequestResponse> listWorkspaceApprovals(Long workspaceId, ApprovalRequest.Status status, Pageable pageable) {
        Page<ApprovalRequest> requests;
        if (status != null) {
            requests = approvalRequestRepository.findByWorkspaceIdAndStatus(workspaceId, status, pageable);
        } else {
            requests = approvalRequestRepository.findByWorkspaceId(workspaceId, pageable);
        }

        return requests.map(r -> {
            List<ApprovalDecision> decisions = approvalDecisionRepository.findByApprovalRequestId(r.getId());
            return ApprovalRequestResponse.from(r, decisions);
        });
    }

    @Transactional(readOnly = true)
    public Page<ApprovalRequestResponse> listPendingForReviewer(Long reviewerId, Pageable pageable) {
        Page<ApprovalRequest> requests = approvalRequestRepository.findPendingByReviewerId(reviewerId, pageable);
        return requests.map(r -> {
            List<ApprovalDecision> decisions = approvalDecisionRepository.findByApprovalRequestId(r.getId());
            return ApprovalRequestResponse.from(r, decisions);
        });
    }
}
