package com.cms.dto.sharing;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateShareLinkRequest {

    private String password;
    private Instant expiresAt;
    private Boolean allowDownload;
    private Boolean watermarkEnabled;
}
