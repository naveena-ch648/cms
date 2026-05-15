package com.cms.service;

import com.cms.entity.Organization;
import com.cms.entity.User;
import com.cms.entity.UserOrganizationRole;
import com.cms.exception.AuthenticationException;
import com.cms.repository.UserOrganizationRoleRepository;
import com.cms.repository.UserRepository;
import com.cms.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserOrganizationRoleRepository userOrgRoleRepository;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate pgJdbc;
    private final AuditService auditService;
    private final PolicyService policyService;
    private final OrganizationService organizationService;
    private final TwoFactorService twoFactorService;

    public AuthService(UserRepository userRepository,
                       UserOrganizationRoleRepository userOrgRoleRepository,
                       JwtProvider jwtProvider,
                       PasswordEncoder passwordEncoder,
                       @Qualifier("pgJdbcTemplate") JdbcTemplate pgJdbc,
                       AuditService auditService,
                       PolicyService policyService,
                       OrganizationService organizationService,
                       TwoFactorService twoFactorService) {
        this.userRepository = userRepository;
        this.userOrgRoleRepository = userOrgRoleRepository;
        this.jwtProvider = jwtProvider;
        this.passwordEncoder = passwordEncoder;
        this.pgJdbc = pgJdbc;
        this.auditService = auditService;
        this.policyService = policyService;
        this.organizationService = organizationService;
        this.twoFactorService = twoFactorService;
    }

    private static final String BLOCKLIST_PREFIX = "jwt:blocklist:";
    private static final String FAILED_ATTEMPTS_PREFIX = "auth:failed:";
    private static final String LOCKOUT_PREFIX = "auth:lockout:";
    private static final String REFRESH_TOKEN_PREFIX = "jwt:refresh:";
    private static final String PENDING_2FA_PREFIX = "auth:pending2fa:";

    @Transactional
    public AuthResult login(String email, Long organizationId, String password, String ipAddress) {
        Organization org = organizationService.getByIdInternal(organizationId);
        Map<String, Object> policy = policyService.getEffectivePolicy(org.getPolicies());

        // Check lockout
        String lockoutKey = LOCKOUT_PREFIX + organizationId + ":" + email;
        Boolean isLocked = false;
        try {
            Integer count = pgJdbc.queryForObject(
                "SELECT COUNT(*) FROM jwt_tokens WHERE jti=? AND token_type='LOCKOUT' AND expires_at>NOW()",
                Integer.class, lockoutKey);
            isLocked = count != null && count > 0;
        } catch (Exception e) {
            log.warn("PG unavailable — skipping lockout check: {}", e.getMessage());
        }
        if (Boolean.TRUE.equals(isLocked)) {
            throw new AuthenticationException("AUTH_ACCOUNT_LOCKED",
                    "Account is temporarily locked. Try again in " +
                            policyService.getAccountLockoutMinutes(policy) + " minutes.");
        }

        User user = userRepository.findByEmailAndOrganizationId(email, organizationId)
                .orElse(null);

        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            handleFailedAttempt(email, organizationId, policy, org, ipAddress);
            throw new AuthenticationException("AUTH_INVALID_CREDENTIALS", "Invalid email or password");
        }

        if (user.getStatus() == User.UserStatus.LOCKED) {
            throw new AuthenticationException("AUTH_ACCOUNT_LOCKED",
                    "Account is locked. Contact your administrator.");
        }

        if (user.getStatus() == User.UserStatus.INACTIVE) {
            throw new AuthenticationException("AUTH_ACCOUNT_INACTIVE", "Account is inactive");
        }

        // Clear failed attempts
        try {
            pgJdbc.update("DELETE FROM jwt_tokens WHERE jti=? AND token_type='FAILED_ATTEMPTS'",
                    FAILED_ATTEMPTS_PREFIX + organizationId + ":" + email);
        } catch (Exception e) {
            log.warn("PG unavailable — could not clear failed attempts: {}", e.getMessage());
        }

        // ── Two-factor authentication check ───────────────────────────────────
        if (twoFactorService.isEnabled(user.getId())) {
            // Issue a short-lived pending token and send OTP if EMAIL method
            String pendingToken = UUID.randomUUID().toString();
            try {
                pgJdbc.update("""
                        INSERT INTO jwt_tokens (jti, token_type, value, expires_at)
                        VALUES (?, 'PENDING_2FA', ?, NOW() + INTERVAL '5 minutes')
                        ON CONFLICT (jti) DO UPDATE SET value=EXCLUDED.value, expires_at=EXCLUDED.expires_at
                        """, PENDING_2FA_PREFIX + pendingToken, String.valueOf(user.getId()));
            } catch (Exception e) {
                log.warn("PG unavailable — 2FA pending token not stored: {}", e.getMessage());
            }

            var method = twoFactorService.getMethod(user.getId()).orElse(null);
            if (method == com.cms.entity.UserTwoFactor.TwoFactorMethod.EMAIL) {
                twoFactorService.sendEmailOtp(user);
            }

            throw new com.cms.exception.TwoFactorRequiredException(pendingToken,
                    method != null ? method.name() : "TOTP");
        }
        // ─────────────────────────────────────────────────────────────────────

        // Get user role
        UserOrganizationRole orgRole = userOrgRoleRepository
                .findByUserIdAndOrganizationId(user.getId(), organizationId).orElse(null);
        List<String> roles = orgRole != null ? List.of(orgRole.getRole().getName()) : List.of();

        // Generate tokens with policy-based session timeout
        int sessionTimeoutMinutes = policyService.getSessionTimeoutMinutes(policy);
        long accessTokenExpirationMs = sessionTimeoutMinutes * 60 * 1000L;
        String accessToken = jwtProvider.generateAccessToken(user.getId(), organizationId, roles, accessTokenExpirationMs);
        String refreshToken = jwtProvider.generateRefreshToken(user.getId());

        // Store refresh token in PostgreSQL
        String refreshJti = jwtProvider.getTokenId(refreshToken);
        try {
            pgJdbc.update("""
                    INSERT INTO jwt_tokens (jti, token_type, value, expires_at)
                    VALUES (?, 'REFRESH', ?, NOW() + INTERVAL '7 days')
                    ON CONFLICT (jti) DO UPDATE SET value=EXCLUDED.value, expires_at=EXCLUDED.expires_at
                    """, REFRESH_TOKEN_PREFIX + refreshJti, String.valueOf(user.getId()));
        } catch (Exception e) {
            log.warn("PG unavailable — refresh token not stored: {}", e.getMessage());
        }

        // Update last login
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        // Audit
        auditService.log(org, user, "LOGIN_SUCCESS", ipAddress);

        return new AuthResult(accessToken, refreshToken, (int) (accessTokenExpirationMs / 1000), user, org, orgRole);
    }

    /**
     * Second step of login when 2FA is enabled.
     * Validates the pending token and the OTP/TOTP code, then issues real tokens.
     */
    @Transactional
    public AuthResult completeTwoFactorLogin(String pendingToken, String code, String ipAddress) {
        String key = PENDING_2FA_PREFIX + pendingToken;
        String storedUserId = null;
        try {
            storedUserId = pgJdbc.queryForObject(
                "SELECT value FROM jwt_tokens WHERE jti=? AND token_type='PENDING_2FA' AND expires_at>NOW()",
                String.class, key);
        } catch (Exception ignored) {}
        if (storedUserId == null) {
            throw new AuthenticationException("AUTH_INVALID_2FA_TOKEN",
                    "2FA session expired or invalid — please log in again");
        }

        Long userId = Long.parseLong(storedUserId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationException("AUTH_INVALID_2FA_TOKEN", "User not found"));

        if (!twoFactorService.verifyCode(user, code)) {
            throw new AuthenticationException("AUTH_INVALID_2FA_CODE", "Invalid verification code");
        }

        pgJdbc.update("DELETE FROM jwt_tokens WHERE jti=?", key);

        Organization org = user.getOrganization();
        Long orgId = org.getId();
        UserOrganizationRole orgRole = userOrgRoleRepository
                .findByUserIdAndOrganizationId(userId, orgId).orElse(null);
        List<String> roles = orgRole != null ? List.of(orgRole.getRole().getName()) : List.of();

        Map<String, Object> policy = policyService.getEffectivePolicy(org.getPolicies());
        int sessionTimeoutMinutes = policyService.getSessionTimeoutMinutes(policy);
        long accessTokenExpirationMs = sessionTimeoutMinutes * 60 * 1000L;

        String accessToken = jwtProvider.generateAccessToken(userId, orgId, roles, accessTokenExpirationMs);
        String refreshToken = jwtProvider.generateRefreshToken(userId);

        String refreshJti = jwtProvider.getTokenId(refreshToken);
        pgJdbc.update("""
                INSERT INTO jwt_tokens (jti, token_type, value, expires_at)
                VALUES (?, 'REFRESH', ?, NOW() + INTERVAL '7 days')
                ON CONFLICT (jti) DO UPDATE SET value=EXCLUDED.value, expires_at=EXCLUDED.expires_at
                """, REFRESH_TOKEN_PREFIX + refreshJti, String.valueOf(userId));

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);
        auditService.log(org, user, "LOGIN_SUCCESS_2FA", ipAddress);

        return new AuthResult(accessToken, refreshToken, (int) (accessTokenExpirationMs / 1000), user, org, orgRole);
    }

    public AuthResult refresh(String refreshToken) {
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new AuthenticationException("AUTH_INVALID_REFRESH_TOKEN", "Invalid or expired refresh token");
        }

        String jti = jwtProvider.getTokenId(refreshToken);
        String storedUserId = null;
        try {
            storedUserId = pgJdbc.queryForObject(
                "SELECT value FROM jwt_tokens WHERE jti=? AND token_type='REFRESH' AND expires_at>NOW()",
                String.class, REFRESH_TOKEN_PREFIX + jti);
        } catch (Exception ignored) {}

        if (storedUserId == null) {
            throw new AuthenticationException("AUTH_INVALID_REFRESH_TOKEN", "Refresh token has been revoked");
        }

        // Invalidate old refresh token
        pgJdbc.update("DELETE FROM jwt_tokens WHERE jti=?", REFRESH_TOKEN_PREFIX + jti);

        Long userId = Long.parseLong(storedUserId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationException("AUTH_INVALID_REFRESH_TOKEN", "User not found"));

        Long orgId = user.getOrganization().getId();
        Organization org = user.getOrganization();
        UserOrganizationRole orgRole = userOrgRoleRepository
                .findByUserIdAndOrganizationId(userId, orgId).orElse(null);
        List<String> roles = orgRole != null ? List.of(orgRole.getRole().getName()) : List.of();

        // Generate new tokens
        Map<String, Object> policy = policyService.getEffectivePolicy(org.getPolicies());
        int sessionTimeoutMinutes = policyService.getSessionTimeoutMinutes(policy);
        long accessTokenExpirationMs = sessionTimeoutMinutes * 60 * 1000L;

        String newAccessToken = jwtProvider.generateAccessToken(userId, orgId, roles, accessTokenExpirationMs);
        String newRefreshToken = jwtProvider.generateRefreshToken(userId);

        String newRefreshJti = jwtProvider.getTokenId(newRefreshToken);
        pgJdbc.update("""
                INSERT INTO jwt_tokens (jti, token_type, value, expires_at)
                VALUES (?, 'REFRESH', ?, NOW() + INTERVAL '7 days')
                ON CONFLICT (jti) DO UPDATE SET value=EXCLUDED.value, expires_at=EXCLUDED.expires_at
                """, REFRESH_TOKEN_PREFIX + newRefreshJti, String.valueOf(userId));

        return new AuthResult(newAccessToken, newRefreshToken, (int) (accessTokenExpirationMs / 1000), user, org, orgRole);
    }

    public void logout(String accessToken) {
        if (jwtProvider.validateToken(accessToken)) {
            String jti = jwtProvider.getTokenId(accessToken);
            long remaining = jwtProvider.getRemainingExpiration(accessToken);
            if (remaining > 0) {
                pgJdbc.update("""
                        INSERT INTO jwt_tokens (jti, token_type, value, expires_at)
                        VALUES (?, 'BLOCKLIST', 'blocked', NOW() + (? * INTERVAL '1 millisecond'))
                        ON CONFLICT (jti) DO NOTHING
                        """, BLOCKLIST_PREFIX + jti, remaining);
            }
        }
    }

    private void handleFailedAttempt(String email, Long orgId, Map<String, Object> policy,
                                     Organization org, String ipAddress) {
        try {
            String failedKey = FAILED_ATTEMPTS_PREFIX + orgId + ":" + email;
            int lockoutMinutes = policyService.getAccountLockoutMinutes(policy);
            int maxAttempts = policyService.getMaxFailedLoginAttempts(policy);

            // Upsert failed attempt counter
            pgJdbc.update("""
                    INSERT INTO jwt_tokens (jti, token_type, value, expires_at)
                    VALUES (?, 'FAILED_ATTEMPTS', '1', NOW() + (? * INTERVAL '1 minute'))
                    ON CONFLICT (jti) DO UPDATE
                      SET value = (CAST(jwt_tokens.value AS INT) + 1)::TEXT,
                          expires_at = NOW() + (? * INTERVAL '1 minute')
                    """, failedKey, lockoutMinutes, lockoutMinutes);

            String countStr = pgJdbc.queryForObject(
                    "SELECT value FROM jwt_tokens WHERE jti=?", String.class, failedKey);
            int attempts = countStr != null ? Integer.parseInt(countStr) : 0;

            if (attempts >= maxAttempts) {
                String lockoutKey = LOCKOUT_PREFIX + orgId + ":" + email;
                pgJdbc.update("""
                        INSERT INTO jwt_tokens (jti, token_type, value, expires_at)
                        VALUES (?, 'LOCKOUT', 'locked', NOW() + (? * INTERVAL '1 minute'))
                        ON CONFLICT (jti) DO UPDATE SET expires_at=EXCLUDED.expires_at
                        """, lockoutKey, lockoutMinutes);
                pgJdbc.update("DELETE FROM jwt_tokens WHERE jti=?", failedKey);
            }
        } catch (Exception e) {
            log.warn("PG unavailable — failed attempt tracking skipped: {}", e.getMessage());
        }

        auditService.log(org, null, "LOGIN_FAILED", ipAddress);
    }

    public record AuthResult(String accessToken, String refreshToken, int expiresIn,
                              User user, Organization organization, UserOrganizationRole orgRole) {}
}
