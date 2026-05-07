package com.cms.dto.workflow;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkTransitionRequest {

    @NotEmpty(message = "File IDs list cannot be empty")
    @Size(max = 100, message = "Cannot bulk transition more than 100 files at once")
    private List<String> fileIds;

    @NotNull(message = "Target state is required")
    private String targetState;

    @Size(max = 1000, message = "Comment must not exceed 1000 characters")
    private String comment;
}
