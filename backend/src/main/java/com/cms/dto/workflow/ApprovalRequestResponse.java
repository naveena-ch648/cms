package com.cms.dto.workflow;

import com.cms.entity.ApprovalDecision;
import com.cms.entity.ApprovalRequest;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalRequestResponse {

    private String id;
    private String fileId;
    private String fileName;
    private String submitterId;
    private String submitterName;
    private String status;
    private String fromState;
    private String toState;
    private String comment;
    private List<ReviewerInfo> reviewers;
    private Instant createdAt;
    private Instant completedAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReviewerInfo {
        private String id;
        private String name;
        private String decision;
        private String comment;
        private Instant decidedAt;

        public static ReviewerInfo from(ApprovalDecision decision) {
            return ReviewerInfo.builder()
                    .id(decision.getReviewer().getUuid())
                    .name(decision.getReviewer().getFirstName() + " " + decision.getReviewer().getLastName())
                    .decision(decision.getDecision().name())
                    .comment(decision.getComment())
                    .decidedAt(decision.getDecidedAt())
                    .build();
        }
    }

    public static ApprovalRequestResponse from(ApprovalRequest request, List<ApprovalDecision> decisions) {
        return ApprovalRequestResponse.builder()
                .id(request.getUuid())
                .fileId(request.getFile().getUuid())
                .fileName(request.getFile().getName())
                .submitterId(request.getSubmitter().getUuid())
                .submitterName(request.getSubmitter().getFirstName() + " " + request.getSubmitter().getLastName())
                .status(request.getStatus().name())
                .fromState(request.getFromState())
                .toState(request.getToState())
                .comment(request.getComment())
                .reviewers(decisions.stream().map(ReviewerInfo::from).collect(Collectors.toList()))
                .createdAt(request.getCreatedAt())
                .completedAt(request.getCompletedAt())
                .build();
    }
}
