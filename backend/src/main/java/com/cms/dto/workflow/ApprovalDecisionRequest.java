package com.cms.dto.workflow;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalDecisionRequest {

    @NotNull(message = "Decision is required")
    private String decision;

    @Size(max = 1000, message = "Comment must not exceed 1000 characters")
    private String comment;
}
