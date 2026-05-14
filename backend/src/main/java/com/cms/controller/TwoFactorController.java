package com.cms.controller;

import com.cms.dto.ApiResponse;
import com.cms.entity.UserTwoFactor.TwoFactorMethod;
import com.cms.security.UserPrincipal;
import com.cms.service.TwoFactorService;
import com.cms.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/me/two-factor")
@RequiredArgsConstructor
public class TwoFactorController {

    private final TwoFactorService twoFactorService;
    private final UserService userService;

    /** GET /api/v1/me/two-factor — current 2FA status */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatus(
            @AuthenticationPrincipal UserPrincipal principal) {
        boolean enabled = twoFactorService.isEnabled(principal.getId());
        TwoFactorMethod method = twoFactorService.getMethod(principal.getId()).orElse(null);
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "enabled", enabled,
                "method", method != null ? method.name() : "NONE"
        )));
    }

    /** POST /api/v1/me/two-factor/totp/init — begin TOTP setup, returns QR URI */
    @PostMapping("/totp/init")
    public ResponseEntity<ApiResponse<Map<String, String>>> initTotp(
            @AuthenticationPrincipal UserPrincipal principal) {
        String otpauthUri = twoFactorService.initiateTotpSetup(
                userService.getByIdInternal(principal.getId()));
        return ResponseEntity.ok(ApiResponse.ok(Map.of("otpauthUri", otpauthUri)));
    }

    /** POST /api/v1/me/two-factor/totp/confirm — verify first code, enable TOTP */
    @PostMapping("/totp/confirm")
    public ResponseEntity<ApiResponse<Map<String, Object>>> confirmTotp(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ConfirmCodeRequest req) {
        List<String> backupCodes = twoFactorService.confirmTotpSetup(
                userService.getByIdInternal(principal.getId()), req.getCode());
        return ResponseEntity.ok(ApiResponse.ok(Map.of("backupCodes", backupCodes)));
    }

    /** POST /api/v1/me/two-factor/email/enable — enable email OTP */
    @PostMapping("/email/enable")
    public ResponseEntity<ApiResponse<Map<String, Object>>> enableEmail(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<String> backupCodes = twoFactorService.enableEmailOtp(
                userService.getByIdInternal(principal.getId()));
        return ResponseEntity.ok(ApiResponse.ok(Map.of("backupCodes", backupCodes)));
    }

    /** POST /api/v1/me/two-factor/disable — disable 2FA */
    @PostMapping("/disable")
    public ResponseEntity<ApiResponse<Map<String, String>>> disable(
            @AuthenticationPrincipal UserPrincipal principal) {
        twoFactorService.disable(userService.getByIdInternal(principal.getId()));
        return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Two-factor authentication disabled")));
    }

    // ─── Request DTOs ─────────────────────────────────────────────────────────

    @Data
    public static class ConfirmCodeRequest {
        @NotNull
        private Integer code;
    }

    @Data
    public static class VerifyCodeRequest {
        @NotBlank
        private String code;
    }
}
