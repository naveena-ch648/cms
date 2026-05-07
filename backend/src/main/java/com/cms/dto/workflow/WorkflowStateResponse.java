package com.cms.dto.workflow;

import com.cms.entity.WorkflowState;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowStateResponse {

    private String currentState;
    private List<String> allowedTransitions;
    private List<String> requiresApproval;
    private boolean hasActiveApproval;
    private String activeApprovalId;
}
