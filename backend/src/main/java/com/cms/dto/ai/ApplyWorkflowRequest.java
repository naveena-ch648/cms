package com.cms.dto.ai;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplyWorkflowRequest {
    @NotBlank
    private String workflowId;
}
