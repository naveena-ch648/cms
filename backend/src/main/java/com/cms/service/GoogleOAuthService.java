package com.cms.service;

import com.cms.config.DataInitializer;
import com.cms.entity.*;
import com.cms.repository.*;
import com.cms.security.JwtProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Handles the server-side Google OAuth 2.0 Authorization Code flow.
 *
 * Flow:
 *  1.  Frontend redirects user to Google's consent screen via /api/v1/auth/google/initiate
 *  2.  Google redirects back to /api/v1/auth/google/callback?code=…&state=…
 *  3.  This service exchanges the code for tokens, fetches the Google profile,
 *      finds-or-creates a matching CMS User, and issues CMS JWT tokens.
 */
@Slf4j
@Service
public class GoogleOAuthService {

    // ── OAuth / Google credentials (populated from application.yml) ─────────
    @Value("${google.oauth.client-id}")
    private String clientId;

    @Value("${google.oauth.client-secret}")
    private String clientSecret;

    @Value("${google.oauth.redirect-uri}")
    private String redirectUri;

    // ── Google endpoints ─────────────────────────────────────────────────────
    private static final String GOOGLE_TOKEN_URL    = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_USERINFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";

    // ── CSRF state prefix stored in PG ───────────────────────────────────────
    private static final String STATE_PREFIX   = "oauth:state:";
    private static final String REFRESH_PREFIX = "jwt:refresh:";

    // ── Default org id that Google-authenticated users join ──────────────────
    private static final long DEFAULT_ORG_ID = 1L;

    private final UserRepository              userRepository;
    private final UserOrganizationRoleRepository userOrgRoleRepository;
    private final OrganizationRepository      organizationRepository;
    private final RoleRepository              roleRepository;
    private final JwtProvider                 jwtProvider;
    private final PasswordEncoder             passwordEncoder;
    private final AuditService                auditService;
    private final JdbcTemplate                pgJdbc;

    private final RestTemplate  restTemplate  = new RestTemplate();
    private final ObjectMapper  objectMapper  = new ObjectMapper();

    public GoogleOAuthService(
            UserRepository userRepository,
            UserOrganizationRoleRepository userOrgRoleRepository,
            OrganizationRepository organizationRepository,
            RoleRepository roleRepository,
            JwtProvider jwtProvider,
            PasswordEncoder passwordEncoder,
            AuditService auditService,
            @Qualifier("pgJdbcTemplate") JdbcTemplate pgJdbc) {
        this.userRepository           = userRepository;
        this.userOrgRoleRepository    = userOrgRoleRepository;
        this.organizationRepository   = organizationRepository;
        this.roleRepository           = roleRepository;
        this.jwtProvider              = jwtProvider;
        this.passwordEncoder          = passwordEncoder;
        this.auditService             = auditService;
        this.pgJdbc                   = pgJdbc;
    }

    // ── 1. Build the Google Authorization URL ────────────────────────────────

    /**
     * Generates a state token, persists it in PG for CSRF protection, and
     * returns the full Google OAuth consent-screen URL.
     */
    public String buildAuthorizationUrl() {
        String state = UUID.randomUUID().toString();
        try {
            pgJdbc.update("""
                    INSERT INTO jwt_tokens (jti, token_type, value, expires_at)
                    VALUES (?, 'OAUTH_STATE', '1', NOW() + INTERVAL '10 minutes')
                    ON CONFLICT (jti) DO UPDATE SET expires_at = EXCLUDED.expires_at
                    """, STATE_PREFIX + state);
        } catch (Exception e) {
            log.warn("PG unavailable — OAuth state not stored (CSRF check will be skipped): {}", e.getMessage());
        }

        return "https://accounts.google.com/o/oauth2/v2/auth" +
               "?client_id="     + encode(clientId) +
               "&redirect_uri="  + encode(redirectUri) +
               "&response_type=code" +
               "&scope="         + encode("openid email profile") +
               "&state="         + state +
               "&access_type=offline" +
               "&prompt=consent";
    }

    // ── 2. Handle the callback ───────────────────────────────────────────────

    @Transactional
    public AuthService.AuthResult handleCallback(String code, String state, String ipAddress) {
        validateState(state);
        GoogleUserInfo googleUser = exchangeCodeForUserInfo(code);
        User user = findOrProvisionUser(googleUser, ipAddress);
        return issueTokens(user, ipAddress);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private void validateState(String state) {
        try {
            Integer count = pgJdbc.queryForObject(
                "SELECT COUNT(*) FROM jwt_tokens WHERE jti=? AND token_type='OAUTH_STATE' AND expires_at>NOW()",
                Integer.class, STATE_PREFIX + state);
            if (count == null || count == 0) {
                throw new com.cms.exception.AuthenticationException(
                    "OAUTH_INVALID_STATE", "Invalid or expired OAuth state parameter");
            }
            // Consume the state token (one-time use)
            pgJdbc.update("DELETE FROM jwt_tokens WHERE jti=?", STATE_PREFIX + state);
        } catch (com.cms.exception.AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            log.warn("PG unavailable — skipping OAuth state validation: {}", e.getMessage());
        }
    }

    /** Exchange the authorization code for an ID token, then fetch user info. */
    private GoogleUserInfo exchangeCodeForUserInfo(String code) {
        // Step A: exchange code for tokens
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code",          code);
        body.add("client_id",     clientId);
        body.add("client_secret", clientSecret);
        body.add("redirect_uri",  redirectUri);
        body.add("grant_type",    "authorization_code");

        ResponseEntity<String> tokenResponse = restTemplate.postForEntity(
                GOOGLE_TOKEN_URL, new HttpEntity<>(body, headers), String.class);

        String accessToken;
        try {
            JsonNode tokenJson = objectMapper.readTree(tokenResponse.getBody());
            accessToken = tokenJson.get("access_token").asText();
        } catch (Exception e) {
            throw new com.cms.exception.AuthenticationException(
                "OAUTH_TOKEN_EXCHANGE_FAILED", "Failed to exchange OAuth code for tokens: " + e.getMessage());
        }

        // Step B: fetch user info from Google
        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.setBearerAuth(accessToken);
        ResponseEntity<String> userInfoResponse = restTemplate.exchange(
                GOOGLE_USERINFO_URL, HttpMethod.GET, new HttpEntity<>(authHeaders), String.class);

        try {
            JsonNode info = objectMapper.readTree(userInfoResponse.getBody());
            return new GoogleUserInfo(
                    info.path("sub").asText(),
                    info.path("email").asText(),
                    info.path("given_name").asText(""),
                    info.path("family_name").asText(""),
                    info.path("picture").asText(""),
                    info.path("email_verified").asBoolean(false));
        } catch (Exception e) {
            throw new com.cms.exception.AuthenticationException(
                "OAUTH_USERINFO_FAILED", "Failed to parse Google user info: " + e.getMessage());
        }
    }

    /** Find an existing CMS user by Google ID or email, or create a new one. */
    private User findOrProvisionUser(GoogleUserInfo googleUser, String ipAddress) {
        if (!googleUser.emailVerified()) {
            throw new com.cms.exception.AuthenticationException(
                "OAUTH_EMAIL_NOT_VERIFIED", "Google account email is not verified");
        }

        Organization org = organizationRepository.findById(DEFAULT_ORG_ID)
                .orElseThrow(() -> new com.cms.exception.AuthenticationException(
                    "OAUTH_NO_ORG", "Default organization not found"));

        // Try to find by googleId first
        User user = userRepository.findByGoogleId(googleUser.sub()).orElse(null);

        if (user == null) {
            // Try to find by email (account merge)
            user = userRepository.findByEmailAndOrganizationId(googleUser.email(), DEFAULT_ORG_ID).orElse(null);
        }

        if (user == null) {
            // Provision new user
            String firstName = googleUser.givenName().isBlank() ? googleUser.email().split("@")[0] : googleUser.givenName();
            String lastName  = googleUser.familyName().isBlank() ? "" : googleUser.familyName();

            user = User.builder()
                    .organization(org)
                    .email(googleUser.email())
                    .googleId(googleUser.sub())
                    // Set a random password hash — Google users authenticate via OAuth only
                    .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .firstName(firstName)
                    .lastName(lastName)
                    .status(User.UserStatus.ACTIVE)
                    .build();
            user = userRepository.save(user);
            final User savedUser = user;

            // Assign default "Viewer" role
            roleRepository.findByNameAndOrganizationId("Viewer", org.getId()).ifPresent(role ->
                userOrgRoleRepository.save(UserOrganizationRole.builder()
                        .userId(savedUser.getId())
                        .organizationId(org.getId())
                        .role(role)
                        .build())
            );
            log.info("Provisioned new Google OAuth user: email={}", googleUser.email());
            auditService.log(org, user, "GOOGLE_OAUTH_REGISTER", ipAddress);
        } else {
            // Merge googleId onto existing account if not already set
            if (user.getGoogleId() == null) {
                user.setGoogleId(googleUser.sub());
            }
            if (user.getStatus() == User.UserStatus.LOCKED || user.getStatus() == User.UserStatus.INACTIVE) {
                throw new com.cms.exception.AuthenticationException(
                    "OAUTH_ACCOUNT_DISABLED", "Your account has been disabled. Contact your administrator.");
            }
        }

        user.setLastLoginAt(Instant.now());
        return userRepository.save(user);
    }

    /** Issue CMS JWT access + refresh tokens for the given user. */
    private AuthService.AuthResult issueTokens(User user, String ipAddress) {
        Organization org = user.getOrganization();
        Long orgId = org.getId();
        UserOrganizationRole orgRole = userOrgRoleRepository
                .findByUserIdAndOrganizationId(user.getId(), orgId).orElse(null);
        List<String> roles = orgRole != null ? List.of(orgRole.getRole().getName()) : List.of();

        long expirationMs = jwtProvider.getAccessTokenExpirationMs();
        String accessToken  = jwtProvider.generateAccessToken(user.getId(), orgId, roles, expirationMs);
        String refreshToken = jwtProvider.generateRefreshToken(user.getId());

        String refreshJti = jwtProvider.getTokenId(refreshToken);
        try {
            pgJdbc.update("""
                    INSERT INTO jwt_tokens (jti, token_type, value, expires_at)
                    VALUES (?, 'REFRESH', ?, NOW() + INTERVAL '7 days')
                    ON CONFLICT (jti) DO UPDATE SET value=EXCLUDED.value, expires_at=EXCLUDED.expires_at
                    """, REFRESH_PREFIX + refreshJti, String.valueOf(user.getId()));
        } catch (Exception e) {
            log.warn("PG unavailable — refresh token not stored: {}", e.getMessage());
        }

        auditService.log(org, user, "GOOGLE_OAUTH_LOGIN", ipAddress);
        return new AuthService.AuthResult(accessToken, refreshToken,
                (int) (expirationMs / 1000), user, org, orgRole);
    }

    // ── URL encoding helper ───────────────────────────────────────────────────
    private static String encode(String value) {
        try {
            return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }

    // ── Value object for Google user info ─────────────────────────────────────
    public record GoogleUserInfo(
            String sub,
            String email,
            String givenName,
            String familyName,
            String picture,
            boolean emailVerified) {}
}
