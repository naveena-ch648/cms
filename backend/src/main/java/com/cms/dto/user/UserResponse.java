package com.cms.dto.user;

import com.cms.entity.User;
import com.cms.entity.UserOrganizationRole;
import lombok.*;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private String status;
    private RoleInfo organizationRole;
    private Instant lastLoginAt;
    private Instant createdAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RoleInfo {
        private String id;
        private String name;
    }

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getUuid())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .status(user.getStatus().name())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .build();
    }

    public static UserResponse from(User user, UserOrganizationRole orgRole) {
        UserResponse response = from(user);
        if (orgRole != null && orgRole.getRole() != null) {
            response.setOrganizationRole(RoleInfo.builder()
                    .id(orgRole.getRole().getUuid())
                    .name(orgRole.getRole().getName())
                    .build());
        }
        return response;
    }
}
