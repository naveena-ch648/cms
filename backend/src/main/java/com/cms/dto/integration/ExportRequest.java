package com.cms.dto.integration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExportRequest {
    @NotBlank
    private String connectionId;

    @NotEmpty
    private List<String> fileIds;

    @NotBlank
    private String targetDriveFolderId;

    private String conflictStrategy; // SKIP, REPLACE, RENAME
}
