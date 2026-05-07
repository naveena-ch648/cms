package com.cms.dto.workflow;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowTransitionRequest {

    @NotNull(message = "Target state is required")
    private String targetState;

    @Size(max = 1000, message = "Comment must not exceed 1000 characters")
    private String comment;
}
