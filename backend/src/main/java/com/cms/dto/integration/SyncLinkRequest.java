package com.cms.dto.integration;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncLinkRequest {
    @NotBlank
    private String connectionId;

    @NotBlank
    private String folderId;

    @NotBlank
    private String externalFolderId;

    private String externalFolderName;

    private String direction; // BIDIRECTIONAL, IMPORT_ONLY, EXPORT_ONLY

    @Min(5)
    private Integer syncIntervalMinutes;
}
