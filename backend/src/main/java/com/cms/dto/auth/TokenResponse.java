package com.cms.dto.auth;

import com.cms.entity.Organization;
import com.cms.entity.User;
import com.cms.entity.UserOrganizationRole;
import com.cms.service.AuthService;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private int expiresIn;
    private UserInfo user;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserInfo {
        private String id;
        private String email;
        private String firstName;
        private String lastName;
        private String organizationId;
        private String organizationName;
    }

    public static TokenResponse from(AuthService.AuthResult result) {
        User user = result.user();
        Organization org = result.organization();

        return TokenResponse.builder()
                .accessToken(result.accessToken())
                .refreshToken(result.refreshToken())
                .tokenType("Bearer")
                .expiresIn(result.expiresIn())
                .user(UserInfo.builder()
                        .id(user.getUuid())
                        .email(user.getEmail())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .organizationId(org.getUuid())
                        .organizationName(org.getName())
                        .build())
                .build();
    }
}
