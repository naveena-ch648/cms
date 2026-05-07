package com.cms.dto.qa;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SummarizeRequest {
    @NotBlank
    private String documentId;

    @NotBlank
    private String workspaceId;

    private String length = "medium"; // short, medium, long

    private Integer maxSections;
}
