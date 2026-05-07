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
public class ImportRequest {
    @NotBlank
    private String connectionId;

    @NotEmpty
    private List<String> driveFileIds;

    @NotBlank
    private String targetFolderId;

    private boolean preserveStructure;
}
