package com.cms.service;

import com.cms.entity.WorkflowState;

import java.util.*;

public final class WorkflowStateMachine {

    private static final Map<WorkflowState, Set<WorkflowState>> TRANSITIONS = Map.of(
            WorkflowState.DRAFT, Set.of(WorkflowState.REVIEW),
            WorkflowState.REVIEW, Set.of(WorkflowState.APPROVED, WorkflowState.DRAFT),
            WorkflowState.APPROVED, Set.of(WorkflowState.PUBLISHED),
            WorkflowState.PUBLISHED, Set.of(WorkflowState.ARCHIVED),
            WorkflowState.ARCHIVED, Set.of(WorkflowState.DRAFT)
    );

    private static final Set<WorkflowState> REQUIRES_APPROVAL = Set.of(WorkflowState.APPROVED);

    private WorkflowStateMachine() {}

    public static Set<WorkflowState> getAllowedTransitions(WorkflowState currentState) {
        return TRANSITIONS.getOrDefault(currentState, Set.of());
    }

    public static boolean isValidTransition(WorkflowState from, WorkflowState to) {
        return getAllowedTransitions(from).contains(to);
    }

    public static boolean requiresApproval(WorkflowState targetState) {
        return REQUIRES_APPROVAL.contains(targetState);
    }

    public static List<WorkflowState> getRequiresApprovalStates(WorkflowState currentState) {
        Set<WorkflowState> allowed = getAllowedTransitions(currentState);
        List<WorkflowState> result = new ArrayList<>();
        for (WorkflowState state : allowed) {
            if (REQUIRES_APPROVAL.contains(state)) {
                result.add(state);
            }
        }
        return result;
    }
}
