package com.cms.dto.permission;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignPermissionRequest {

    private String userUuid;
    private String groupUuid;

    @NotBlank(message = "roleUuid is required")
    private String roleUuid;

    @Builder.Default
    private boolean isOverride = false;
}
