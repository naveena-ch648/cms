package com.cms.dto.qa;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AskRequest {

    @NotBlank(message = "Question is required")
    @Size(max = 2000, message = "Question must not exceed 2000 characters")
    private String question;

    @NotNull(message = "Workspace ID is required")
    private String workspaceId;

    private String conversationId;

    private Integer maxChunks = 5;
}
