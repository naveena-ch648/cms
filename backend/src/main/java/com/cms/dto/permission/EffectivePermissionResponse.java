package com.cms.dto.permission;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EffectivePermissionResponse {

    private String folderUuid;
    private String effectiveRole;
    private String source; // DIRECT, INHERITED, GROUP
    private String sourceFolderUuid;
}
