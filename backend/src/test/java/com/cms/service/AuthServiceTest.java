package com.cms.service;

import com.cms.entity.Organization;
import com.cms.entity.User;
import com.cms.entity.UserOrganizationRole;
import com.cms.exception.AuthenticationException;
import com.cms.exception.TwoFactorRequiredException;
import com.cms.repository.UserOrganizationRoleRepository;
import com.cms.repository.UserRepository;
import com.cms.security.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock UserOrganizationRoleRepository userOrgRoleRepository;
    @Mock JwtProvider jwtProvider;
    @Mock PasswordEncoder passwordEncoder;
    @Mock RedisTemplate<String, String> redisTemplate;
    @Mock ValueOperations<String, String> valueOps;
    @Mock AuditService auditService;
    @Mock PolicyService policyService;
    @Mock OrganizationService organizationService;
    @Mock TwoFactorService twoFactorService;

    @InjectMocks AuthService authService;

    private Organization org;
    private User user;

    @BeforeEach
    void setUp() {
        org = new Organization();
        org.setId(1L);
        org.setName("Test Org");
        org.setSlug("test-org");
        org.setBillingContactEmail("admin@test.com");
        org.setStatus(Organization.OrganizationStatus.ACTIVE);

        user = new User();
        user.setId(10L);
        user.setEmail("user@test.com");
        user.setPasswordHash("hashed");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setStatus(User.UserStatus.ACTIVE);
        user.setOrganization(org);

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        when(organizationService.getByIdInternal(1L)).thenReturn(org);
        when(policyService.getEffectivePolicy(any())).thenReturn(Map.of());
        when(policyService.getSessionTimeoutMinutes(any())).thenReturn(15);
        when(policyService.getAccountLockoutMinutes(any())).thenReturn(15);
        when(policyService.getMaxFailedLoginAttempts(any())).thenReturn(5);
    }

    @Test
    void login_withValidCredentials_returnsAuthResult() {
        when(userRepository.findByEmailAndOrganizationId("user@test.com", 1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "hashed")).thenReturn(true);
        when(twoFactorService.isEnabled(10L)).thenReturn(false);
        when(jwtProvider.generateAccessToken(anyLong(), anyLong(), any(), anyLong())).thenReturn("access-token");
        when(jwtProvider.generateRefreshToken(anyLong())).thenReturn("refresh-token");
        when(jwtProvider.getTokenId("refresh-token")).thenReturn("jti-123");
        when(userOrgRoleRepository.findByUserIdAndOrganizationId(10L, 1L)).thenReturn(Optional.empty());

        AuthService.AuthResult result = authService.login("user@test.com", 1L, "password", "127.0.0.1");

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        assertThat(result.user().getEmail()).isEqualTo("user@test.com");
    }

    @Test
    void login_withWrongPassword_throwsAuthenticationException() {
        when(userRepository.findByEmailAndOrganizationId("user@test.com", 1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);
        when(valueOps.increment(anyString())).thenReturn(1L);

        assertThatThrownBy(() -> authService.login("user@test.com", 1L, "wrong", "127.0.0.1"))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    void login_withUnknownUser_throwsAuthenticationException() {
        when(userRepository.findByEmailAndOrganizationId("unknown@test.com", 1L)).thenReturn(Optional.empty());
        when(valueOps.increment(anyString())).thenReturn(1L);

        assertThatThrownBy(() -> authService.login("unknown@test.com", 1L, "any", "127.0.0.1"))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    void login_withLockedAccount_throwsAuthenticationException() {
        when(redisTemplate.hasKey(contains("auth:lockout:"))).thenReturn(true);

        assertThatThrownBy(() -> authService.login("user@test.com", 1L, "password", "127.0.0.1"))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("locked");
    }

    @Test
    void login_withInactiveUser_throwsAuthenticationException() {
        user.setStatus(User.UserStatus.INACTIVE);
        when(userRepository.findByEmailAndOrganizationId("user@test.com", 1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "hashed")).thenReturn(true);

        assertThatThrownBy(() -> authService.login("user@test.com", 1L, "password", "127.0.0.1"))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("inactive");
    }

    @Test
    void login_with2FAEnabled_throwsTwoFactorRequiredException() {
        when(userRepository.findByEmailAndOrganizationId("user@test.com", 1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "hashed")).thenReturn(true);
        when(twoFactorService.isEnabled(10L)).thenReturn(true);
        when(twoFactorService.getMethod(10L)).thenReturn(Optional.of(com.cms.entity.UserTwoFactor.TwoFactorMethod.TOTP));
        doNothing().when(valueOps).set(anyString(), anyString(), any());

        assertThatThrownBy(() -> authService.login("user@test.com", 1L, "password", "127.0.0.1"))
                .isInstanceOf(TwoFactorRequiredException.class);
    }

    @Test
    void logout_withValidToken_addsToBlocklist() {
        when(jwtProvider.validateToken("valid-token")).thenReturn(true);
        when(jwtProvider.getTokenId("valid-token")).thenReturn("jti-abc");
        when(jwtProvider.getRemainingExpiration("valid-token")).thenReturn(60000L);

        authService.logout("valid-token");

        verify(valueOps).set(eq("jwt:blocklist:jti-abc"), eq("blocked"), any());
    }
}
