package com.cms.dto.workflow;

import com.cms.entity.WorkflowTransition;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowTransitionResponse {

    private String id;
    private String fileId;
    private String fromState;
    private String toState;
    private String actorId;
    private String actorName;
    private String comment;
    private String approvalRequestId;
    private Instant createdAt;

    public static WorkflowTransitionResponse from(WorkflowTransition transition) {
        return WorkflowTransitionResponse.builder()
                .id(transition.getUuid())
                .fileId(transition.getFile().getUuid())
                .fromState(transition.getFromState())
                .toState(transition.getToState())
                .actorId(transition.getActor().getUuid())
                .actorName(transition.getActor().getFirstName() + " " + transition.getActor().getLastName())
                .comment(transition.getComment())
                .approvalRequestId(transition.getApprovalRequest() != null ? transition.getApprovalRequest().getUuid() : null)
                .createdAt(transition.getCreatedAt())
                .build();
    }
}
