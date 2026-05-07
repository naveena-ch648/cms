package com.cms.service;

import com.cms.dto.workflow.WorkflowStateResponse;
import com.cms.dto.workflow.WorkflowTransitionResponse;
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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowService {

    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final WorkflowTransitionRepository workflowTransitionRepository;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final TriggerService triggerService;
    private final ActivityEventService activityEventService;

    @Transactional
    public WorkflowTransitionResponse transition(String fileUuid, String targetStateStr, String comment, Long actorId) {
        FileEntity file = fileRepository.findByUuid(fileUuid)
                .orElseThrow(() -> new ResourceNotFoundException("File not found: " + fileUuid));

        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        WorkflowState currentState = file.getWorkflowState();
        WorkflowState targetState;
        try {
            targetState = WorkflowState.valueOf(targetStateStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("INVALID_STATE", "Invalid workflow state: " + targetStateStr);
        }

        if (!WorkflowStateMachine.isValidTransition(currentState, targetState)) {
            throw new BusinessRuleException("INVALID_TRANSITION",
                    "Invalid transition: cannot move from " + currentState + " to " + targetState);
        }

        if (WorkflowStateMachine.requiresApproval(targetState)) {
            throw new BusinessRuleException("REQUIRES_APPROVAL",
                    "Transition from " + currentState + " to " + targetState + " requires approval. Submit an approval request instead.");
        }

        // Execute prerequisite triggers before transition
        triggerService.executeTriggers(fileUuid, targetState.name());

        return performTransition(file, currentState, targetState, actor, comment, null);
    }

    @Transactional
    public WorkflowTransitionResponse transitionViaApproval(FileEntity file, WorkflowState targetState, User actor, ApprovalRequest approvalRequest) {
        WorkflowState currentState = file.getWorkflowState();
        return performTransition(file, currentState, targetState, actor, "Approved by all reviewers", approvalRequest);
    }

    @Transactional
    public WorkflowTransitionResponse rejectToState(FileEntity file, WorkflowState targetState, User actor, String comment) {
        WorkflowState currentState = file.getWorkflowState();
        return performTransition(file, currentState, targetState, actor, comment, null);
    }

    @Transactional
    public int bulkTransition(List<String> fileUuids, String targetStateStr, String comment, Long actorId) {
        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        WorkflowState targetState;
        try {
            targetState = WorkflowState.valueOf(targetStateStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("INVALID_STATE", "Invalid workflow state: " + targetStateStr);
        }

        List<FileEntity> files = fileUuids.stream()
                .map(uuid -> fileRepository.findByUuid(uuid)
                        .orElseThrow(() -> new ResourceNotFoundException("File not found: " + uuid)))
                .collect(Collectors.toList());

        if (files.isEmpty()) {
            throw new BusinessRuleException("EMPTY_LIST", "No files specified for bulk transition");
        }

        // All files must be in the same state
        Set<WorkflowState> states = files.stream()
                .map(FileEntity::getWorkflowState)
                .collect(Collectors.toSet());

        if (states.size() > 1) {
            String stateList = states.stream().map(Enum::name).collect(Collectors.joining(", "));
            throw new BusinessRuleException("MIXED_STATES",
                    "All files must be in the same state for bulk transition. Found: " + stateList);
        }

        WorkflowState currentState = states.iterator().next();
        if (!WorkflowStateMachine.isValidTransition(currentState, targetState)) {
            throw new BusinessRuleException("INVALID_TRANSITION",
                    "Invalid transition: cannot move from " + currentState + " to " + targetState);
        }

        if (WorkflowStateMachine.requiresApproval(targetState)) {
            throw new BusinessRuleException("REQUIRES_APPROVAL",
                    "Transition to " + targetState + " requires approval. Submit approval requests individually.");
        }

        for (FileEntity file : files) {
            performTransition(file, currentState, targetState, actor, comment, null);
        }

        return files.size();
    }

    @Transactional(readOnly = true)
    public Page<WorkflowTransitionResponse> getHistory(String fileUuid, Pageable pageable) {
        FileEntity file = fileRepository.findByUuid(fileUuid)
                .orElseThrow(() -> new ResourceNotFoundException("File not found: " + fileUuid));

        return workflowTransitionRepository.findByFileIdOrderByCreatedAtDesc(file.getId(), pageable)
                .map(WorkflowTransitionResponse::from);
    }

    @Transactional(readOnly = true)
    public WorkflowStateResponse getState(String fileUuid) {
        FileEntity file = fileRepository.findByUuid(fileUuid)
                .orElseThrow(() -> new ResourceNotFoundException("File not found: " + fileUuid));

        WorkflowState currentState = file.getWorkflowState();
        Set<WorkflowState> allowed = WorkflowStateMachine.getAllowedTransitions(currentState);
        List<WorkflowState> requiresApproval = WorkflowStateMachine.getRequiresApprovalStates(currentState);

        var activeApproval = approvalRequestRepository.findByFileIdAndStatus(file.getId(), ApprovalRequest.Status.PENDING);

        return WorkflowStateResponse.builder()
                .currentState(currentState.name())
                .allowedTransitions(allowed.stream().map(Enum::name).collect(Collectors.toList()))
                .requiresApproval(requiresApproval.stream().map(Enum::name).collect(Collectors.toList()))
                .hasActiveApproval(activeApproval.isPresent())
                .activeApprovalId(activeApproval.map(ApprovalRequest::getUuid).orElse(null))
                .build();
    }

    private WorkflowTransitionResponse performTransition(FileEntity file, WorkflowState fromState, WorkflowState toState,
                                                          User actor, String comment, ApprovalRequest approvalRequest) {
        file.setWorkflowState(toState);
        fileRepository.save(file);

        WorkflowTransition transition = WorkflowTransition.builder()
                .file(file)
                .fromState(fromState.name())
                .toState(toState.name())
                .actor(actor)
                .comment(comment)
                .approvalRequest(approvalRequest)
                .build();

        transition = workflowTransitionRepository.save(transition);
        log.info("Workflow transition: file={} from={} to={} actor={}", file.getUuid(), fromState, toState, actor.getId());

        // Record activity event
        activityEventService.recordEvent(actor, ActivityEvent.ActionType.WORKFLOW_TRANSITIONED,
                "FILE", file.getUuid(), file.getName(),
                file.getWorkspace(), file.getWorkspace().getOrganization(), null);

        return WorkflowTransitionResponse.from(transition);
    }
}
