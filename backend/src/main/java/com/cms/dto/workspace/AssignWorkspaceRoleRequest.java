package com.cms.dto.workspace;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignWorkspaceRoleRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotBlank(message = "Role ID is required")
    private String roleId;
}
