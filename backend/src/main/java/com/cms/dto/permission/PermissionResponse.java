package com.cms.dto.permission;

import com.cms.entity.FolderPermission;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermissionResponse {

    private Long id;
    private String folderUuid;
    private String userUuid;
    private String userName;
    private String groupUuid;
    private String groupName;
    private String role;
    private boolean isOverride;
    private boolean inherited;
    private String sourceFolderUuid;
    private Instant createdAt;

    public static PermissionResponse from(FolderPermission fp) {
        return PermissionResponse.builder()
                .id(fp.getId())
                .folderUuid(fp.getFolder().getUuid())
                .userUuid(fp.getUser() != null ? fp.getUser().getUuid() : null)
                .userName(fp.getUser() != null ? fp.getUser().getFirstName() + " " + fp.getUser().getLastName() : null)
                .groupUuid(fp.getGroup() != null ? fp.getGroup().getUuid() : null)
                .groupName(fp.getGroup() != null ? fp.getGroup().getName() : null)
                .role(fp.getRole().getName())
                .isOverride(fp.isOverride())
                .inherited(false)
                .createdAt(fp.getCreatedAt())
                .build();
    }

    public static PermissionResponse inherited(FolderPermission fp, String sourceFolderUuid) {
        PermissionResponse resp = from(fp);
        resp.setInherited(true);
        resp.setSourceFolderUuid(sourceFolderUuid);
        return resp;
    }
}
