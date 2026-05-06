package com.cms.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangeRoleRequest {
    @NotBlank(message = "Role ID is required")
    private String roleId;
}
