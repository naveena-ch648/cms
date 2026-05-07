package com.cms.dto.workflow;

import com.cms.entity.ApprovalDecision;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalDecisionResponse {

    private String id;
    private String approvalRequestId;
    private String reviewerId;
    private String reviewerName;
    private String decision;
    private String comment;
    private Instant decidedAt;
    private String approvalStatus;
    private long approvedCount;
    private long totalReviewers;

    public static ApprovalDecisionResponse from(ApprovalDecision decision, String approvalStatus, long approvedCount, long totalReviewers) {
        return ApprovalDecisionResponse.builder()
                .id(decision.getUuid())
                .approvalRequestId(decision.getApprovalRequest().getUuid())
                .reviewerId(decision.getReviewer().getUuid())
                .reviewerName(decision.getReviewer().getFirstName() + " " + decision.getReviewer().getLastName())
                .decision(decision.getDecision().name())
                .comment(decision.getComment())
                .decidedAt(decision.getDecidedAt())
                .approvalStatus(approvalStatus)
                .approvedCount(approvedCount)
                .totalReviewers(totalReviewers)
                .build();
    }
}
