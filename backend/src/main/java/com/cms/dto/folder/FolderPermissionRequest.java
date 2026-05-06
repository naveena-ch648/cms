package com.cms.dto.folder;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FolderPermissionRequest {

    private String userId;

    private String groupId;

    @NotNull(message = "Role ID is required")
    private String roleId;
}
