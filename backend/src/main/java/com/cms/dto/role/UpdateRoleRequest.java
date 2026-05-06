package com.cms.dto.role;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateRoleRequest {
    private String name;
    private String description;
    private String parentRoleId;
    private List<Long> permissionIds;
}
