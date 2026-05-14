package com.cms.dto.fileshare;

import com.cms.entity.FileShare;
import lombok.*;

import java.time.Instant;

/** Describes a single share record (who a file is shared with). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileShareResponse {

    private String uuid;
    private String fileUuid;
    private String fileName;

    private UserInfo sharedBy;
    private UserInfo sharedWith;

    private String permission;
    private boolean allowDownload;
    private boolean watermarkEnabled;
    private Instant expiresAt;
    private String status;
    private Instant createdAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserInfo {
        private String id;
        private String email;
        private String firstName;
        private String lastName;
    }

    public static FileShareResponse from(FileShare share) {
        return FileShareResponse.builder()
                .uuid(share.getUuid())
                .fileUuid(share.getFile().getUuid())
                .fileName(share.getFile().getName())
                .sharedBy(UserInfo.builder()
                        .id(share.getSharedBy().getUuid())
                        .email(share.getSharedBy().getEmail())
                        .firstName(share.getSharedBy().getFirstName())
                        .lastName(share.getSharedBy().getLastName())
                        .build())
                .sharedWith(UserInfo.builder()
                        .id(share.getSharedWith().getUuid())
                        .email(share.getSharedWith().getEmail())
                        .firstName(share.getSharedWith().getFirstName())
                        .lastName(share.getSharedWith().getLastName())
                        .build())
                .permission(share.getPermission().name())
                .allowDownload(share.isAllowDownload())
                .watermarkEnabled(share.isWatermarkEnabled())
                .expiresAt(share.getExpiresAt())
                .status(share.getStatus().name())
                .createdAt(share.getCreatedAt())
                .build();
    }
}
