package com.cms.dto.fileshare;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateFileShareRequest {

    /** VIEWER or EDITOR — null means no change */
    private String permission;
    private Boolean allowDownload;
    private Boolean watermarkEnabled;
    /** null means remove expiry, non-null sets new expiry */
    private Instant expiresAt;
    private Boolean removeExpiry;
}
