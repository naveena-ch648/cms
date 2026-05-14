package com.cms.controller;

import com.cms.annotation.Audited;
import com.cms.dto.ApiResponse;
import com.cms.dto.auth.*;
import com.cms.entity.AuditCategory;
import com.cms.entity.AuditEventType;
import com.cms.entity.User;
import com.cms.entity.UserOrganizationRole;
import com.cms.repository.UserOrganizationRoleRepository;
import com.cms.security.UserPrincipal;
import com.cms.service.AuthService;
import com.cms.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final UserOrganizationRoleRepository userOrgRoleRepository;

    @PostMapping("/login")
    @Audited(event = AuditEventType.LOGIN_SUCCESS, category = AuditCategory.AUTHENTICATION)
    public ResponseEntity<ApiResponse<TokenResponse>> login(
            @Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        // For now, login against the platform org (id=1). Multi-tenant login
        // can be extended with an organizationId parameter or subdomain detection.
        AuthService.AuthResult result = authService.login(
                request.getEmail(), 1L, request.getPassword(), httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.ok(TokenResponse.from(result)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(
            @Valid @RequestBody RefreshRequest request) {
        AuthService.AuthResult result = authService.refresh(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.ok(TokenResponse.from(result)));
    }

    @PostMapping("/logout")
    @Audited(event = AuditEventType.LOGOUT, category = AuditCategory.AUTHENTICATION)
    public ResponseEntity<ApiResponse<Map<String, String>>> logout(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            authService.logout(bearerToken.substring(7));
        }
        return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Successfully signed out")));
    }

    @PostMapping("/two-factor/verify")
    public ResponseEntity<ApiResponse<TokenResponse>> verifyTwoFactor(
            @Valid @RequestBody TwoFactorVerifyRequest request, HttpServletRequest httpRequest) {
        AuthService.AuthResult result = authService.completeTwoFactorLogin(
                request.getPendingToken(), request.getCode(), httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.ok(TokenResponse.from(result)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> me(
            @AuthenticationPrincipal UserPrincipal principal) {
        User user = userService.getByIdInternal(principal.getId());
        UserOrganizationRole orgRole = userOrgRoleRepository
                .findByUserIdAndOrganizationId(user.getId(), user.getOrganization().getId())
                .orElse(null);

        Map<String, Object> profile = Map.of(
                "id", user.getUuid(),
                "email", user.getEmail(),
                "firstName", user.getFirstName(),
                "lastName", user.getLastName(),
                "status", user.getStatus().name(),
                "organizationId", user.getOrganization().getUuid(),
                "organizationName", user.getOrganization().getName(),
                "organizationRole", orgRole != null ? orgRole.getRole().getName() : "None"
        );
        return ResponseEntity.ok(ApiResponse.ok(profile));
    }

    @Data
    public static class TwoFactorVerifyRequest {
        @NotBlank
        private String pendingToken;
        @NotBlank
        private String code;
    }
}
