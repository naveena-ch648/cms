package com.cms.dto.sharing;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShareLinkAccessResponse {

    private Instant accessedAt;
    private String ipAddress;
    private String userAgent;
}
