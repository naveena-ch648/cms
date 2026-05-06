package com.cms.dto.sharing;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateShareLinkRequest {

    @NotNull(message = "resourceType is required")
    private String resourceType; // FILE or FOLDER

    private String fileUuid;
    private String folderUuid;
    private String password;
    private Instant expiresAt;

    @Builder.Default
    private boolean allowDownload = true;

    @Builder.Default
    private boolean watermarkEnabled = false;
}
