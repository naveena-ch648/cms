package com.cms.dto.workflow;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalSubmitRequest {

    @NotEmpty(message = "At least one reviewer is required")
    @Size(max = 20, message = "Cannot have more than 20 reviewers")
    private List<String> reviewerIds;

    @Size(max = 1000, message = "Comment must not exceed 1000 characters")
    private String comment;
}
