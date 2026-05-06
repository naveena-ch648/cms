package com.cms.dto.role;

import com.cms.entity.Permission;
import com.cms.entity.Role;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleResponse {

    private String id;
    private String name;
    private String description;
    private boolean system;
    private ParentRoleInfo parentRole;
    private List<PermissionInfo> permissions;
    private Instant createdAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ParentRoleInfo {
        private String id;
        private String name;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PermissionInfo {
        private Long id;
        private String name;
        private String description;
        private String category;
    }

    public static RoleResponse from(Role role) {
        RoleResponse.RoleResponseBuilder builder = RoleResponse.builder()
                .id(role.getUuid())
                .name(role.getName())
                .description(role.getDescription())
                .system(role.isSystem())
                .createdAt(role.getCreatedAt());

        if (role.getParentRole() != null) {
            builder.parentRole(ParentRoleInfo.builder()
                    .id(role.getParentRole().getUuid())
                    .name(role.getParentRole().getName())
                    .build());
        }

        Set<Permission> perms = role.getPermissions();
        if (perms != null) {
            builder.permissions(perms.stream()
                    .map(p -> PermissionInfo.builder()
                            .id(p.getId())
                            .name(p.getName())
                            .description(p.getDescription())
                            .category(p.getCategory())
                            .build())
                    .toList());
        }

        return builder.build();
    }
}
