package com.cms.dto.fileshare;

import com.cms.entity.FileShare;
import lombok.*;

import java.time.Instant;

/** Represents a file shared WITH the current user — for the Shared Files page. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedWithMeResponse {

    private String shareUuid;
    private String permission;
    private boolean allowDownload;
    private boolean watermarkEnabled;
    private Instant expiresAt;
    private Instant sharedAt;

    // File details
    private String fileUuid;
    private String fileName;
    private Long fileSizeBytes;
    private String fileMimeType;
    private String fileThumbnailUrl;
    private String fileWorkspaceId;
    private String fileFolderId;

    // Who shared
    private String sharedByUserId;
    private String sharedByFirstName;
    private String sharedByLastName;
    private String sharedByEmail;

    public static SharedWithMeResponse from(FileShare share) {
        var file = share.getFile();
        var by   = share.getSharedBy();
        return SharedWithMeResponse.builder()
                .shareUuid(share.getUuid())
                .permission(share.getPermission().name())
                .allowDownload(share.isAllowDownload())
                .watermarkEnabled(share.isWatermarkEnabled())
                .expiresAt(share.getExpiresAt())
                .sharedAt(share.getCreatedAt())
                .fileUuid(file.getUuid())
                .fileName(file.getName())
                .fileSizeBytes(file.getSizeBytes())
                .fileMimeType(file.getMimeType())
                .fileThumbnailUrl(null) // resolved by frontend via separate preview API if needed
                .fileWorkspaceId(file.getWorkspace() != null ? file.getWorkspace().getUuid() : null)
                .fileFolderId(file.getFolder() != null ? file.getFolder().getUuid() : null)
                .sharedByUserId(by.getUuid())
                .sharedByFirstName(by.getFirstName())
                .sharedByLastName(by.getLastName())
                .sharedByEmail(by.getEmail())
                .build();
    }
}
