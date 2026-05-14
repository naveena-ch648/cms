package com.cms.dto.fileshare;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateFileShareRequest {

    /** UUID of the user to share with */
    @NotBlank(message = "sharedWithUserUuid is required")
    private String sharedWithUserUuid;

    /** VIEWER or EDITOR */
    @NotNull(message = "permission is required")
    private String permission;

    private boolean allowDownload = true;
    private boolean watermarkEnabled = false;

    /** ISO-8601 instant — null means no expiry */
    private Instant expiresAt;
}
