package com.cms.dto.folder;

import com.cms.entity.FolderPermission;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FolderPermissionResponse {

    private Long id;
    private String folderId;
    private String userId;
    private String userName;
    private String groupId;
    private String groupName;
    private String roleId;
    private String roleName;
    private boolean inherited;
    private String inheritedFrom;
    private Instant createdAt;

    public static FolderPermissionResponse from(FolderPermission fp) {
        return FolderPermissionResponse.builder()
                .id(fp.getId())
                .folderId(fp.getFolder().getUuid())
                .userId(fp.getUser() != null ? fp.getUser().getUuid() : null)
                .userName(fp.getUser() != null ? fp.getUser().getFirstName() + " " + fp.getUser().getLastName() : null)
                .groupId(fp.getGroup() != null ? fp.getGroup().getUuid() : null)
                .groupName(fp.getGroup() != null ? fp.getGroup().getName() : null)
                .roleId(fp.getRole().getUuid())
                .roleName(fp.getRole().getName())
                .inherited(false)
                .createdAt(fp.getCreatedAt())
                .build();
    }

    public static FolderPermissionResponse inherited(FolderPermission fp, String inheritedFromFolderUuid) {
        FolderPermissionResponse response = from(fp);
        response.setInherited(true);
        response.setInheritedFrom(inheritedFromFolderUuid);
        return response;
    }
}
