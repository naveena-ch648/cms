package com.cms.dto.sharing;

import com.cms.entity.SharedLink;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShareLinkResponse {

    private String uuid;
    private String token;
    private String url;
    private String resourceType;
    private String resourceName;
    private boolean hasPassword;
    private Instant expiresAt;
    private boolean allowDownload;
    private boolean watermarkEnabled;
    private String status;
    private int viewCount;
    private Instant lastAccessedAt;
    private Instant createdAt;

    public static ShareLinkResponse from(SharedLink link, String baseUrl) {
        return ShareLinkResponse.builder()
                .uuid(link.getUuid())
                .token(link.getToken())
                .url(baseUrl + "/share/" + link.getToken())
                .resourceType(link.getResourceType().name())
                .resourceName(getResourceName(link))
                .hasPassword(link.getPasswordHash() != null)
                .expiresAt(link.getExpiresAt())
                .allowDownload(link.isAllowDownload())
                .watermarkEnabled(link.isWatermarkEnabled())
                .status(link.getStatus().name())
                .viewCount(link.getViewCount())
                .lastAccessedAt(link.getLastAccessedAt())
                .createdAt(link.getCreatedAt())
                .build();
    }

    private static String getResourceName(SharedLink link) {
        if (link.getFile() != null) return link.getFile().getName();
        if (link.getFolder() != null) return link.getFolder().getName();
        return "Unknown";
    }
}
