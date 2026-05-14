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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserOrganizationRoleRepository userOrgRoleRepository;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, String> redisTemplate;
    private final AuditService auditService;
    private final PolicyService policyService;
    private final OrganizationService organizationService;
    private final TwoFactorService twoFactorService;

    private static final String BLOCKLIST_PREFIX = "jwt:blocklist:";
    private static final String FAILED_ATTEMPTS_PREFIX = "auth:failed:";
    private static final String LOCKOUT_PREFIX = "auth:lockout:";
    private static final String REFRESH_TOKEN_PREFIX = "jwt:refresh:";
    private static final String PENDING_2FA_PREFIX = "auth:pending2fa:";

    @Transactional
    public AuthResult login(String email, Long organizationId, String password, String ipAddress) {
        Organization org = organizationService.getByIdInternal(organizationId);
        Map<String, Object> policy = policyService.getEffectivePolicy(org.getPolicies());

        // Check lockout (skip gracefully if Redis is unavailable)
        String lockoutKey = LOCKOUT_PREFIX + organizationId + ":" + email;
        Boolean isLocked = false;
        try {
            isLocked = redisTemplate.hasKey(lockoutKey);
        } catch (Exception e) {
            log.warn("Redis unavailable — skipping lockout check: {}", e.getMessage());
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
            redisTemplate.delete(FAILED_ATTEMPTS_PREFIX + organizationId + ":" + email);
        } catch (Exception e) {
            log.warn("Redis unavailable — could not clear failed attempts: {}", e.getMessage());
        }

        // ── Two-factor authentication check ───────────────────────────────────
        if (twoFactorService.isEnabled(user.getId())) {
            // Issue a short-lived pending token and send OTP if EMAIL method
            String pendingToken = UUID.randomUUID().toString();
            try {
                redisTemplate.opsForValue().set(
                        PENDING_2FA_PREFIX + pendingToken,
                        String.valueOf(user.getId()),
                        Duration.ofMinutes(5)
                );
            } catch (Exception e) {
                log.warn("Redis unavailable — 2FA pending token not stored: {}", e.getMessage());
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

        // Store refresh token in Redis (best-effort — skip if unavailable)
        String refreshJti = jwtProvider.getTokenId(refreshToken);
        try {
            redisTemplate.opsForValue().set(
                    REFRESH_TOKEN_PREFIX + refreshJti,
                    String.valueOf(user.getId()),
                    7, TimeUnit.DAYS
            );
        } catch (Exception e) {
            log.warn("Redis unavailable — refresh token not stored: {}", e.getMessage());
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
        String storedUserId = redisTemplate.opsForValue().get(key);
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

        redisTemplate.delete(key);

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
        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + refreshJti,
                String.valueOf(userId),
                7, TimeUnit.DAYS
        );

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
        String storedUserId = redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + jti);

        if (storedUserId == null) {
            throw new AuthenticationException("AUTH_INVALID_REFRESH_TOKEN", "Refresh token has been revoked");
        }

        // Invalidate old refresh token
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + jti);

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

        // Store new refresh token
        String newRefreshJti = jwtProvider.getTokenId(newRefreshToken);
        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + newRefreshJti,
                String.valueOf(userId),
                7, TimeUnit.DAYS
        );

        return new AuthResult(newAccessToken, newRefreshToken, (int) (accessTokenExpirationMs / 1000), user, org, orgRole);
    }

    public void logout(String accessToken) {
        if (jwtProvider.validateToken(accessToken)) {
            String jti = jwtProvider.getTokenId(accessToken);
            long remaining = jwtProvider.getRemainingExpiration(accessToken);
            if (remaining > 0) {
                redisTemplate.opsForValue().set(
                        BLOCKLIST_PREFIX + jti, "blocked",
                        remaining, TimeUnit.MILLISECONDS
                );
            }
        }
    }

    private void handleFailedAttempt(String email, Long orgId, Map<String, Object> policy,
                                     Organization org, String ipAddress) {
        try {
            String failedKey = FAILED_ATTEMPTS_PREFIX + orgId + ":" + email;
            Long attempts = redisTemplate.opsForValue().increment(failedKey);
            redisTemplate.expire(failedKey, Duration.ofMinutes(policyService.getAccountLockoutMinutes(policy)));

            int maxAttempts = policyService.getMaxFailedLoginAttempts(policy);
            if (attempts != null && attempts >= maxAttempts) {
                String lockoutKey = LOCKOUT_PREFIX + orgId + ":" + email;
                redisTemplate.opsForValue().set(lockoutKey, "locked",
                        Duration.ofMinutes(policyService.getAccountLockoutMinutes(policy)));
                redisTemplate.delete(failedKey);
            }
        } catch (Exception e) {
            log.warn("Redis unavailable — failed attempt tracking skipped: {}", e.getMessage());
        }

        auditService.log(org, null, "LOGIN_FAILED", ipAddress);
    }

    public record AuthResult(String accessToken, String refreshToken, int expiresIn,
                              User user, Organization organization, UserOrganizationRole orgRole) {}
}
