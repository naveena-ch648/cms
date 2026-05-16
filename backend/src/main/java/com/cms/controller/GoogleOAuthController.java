package com.cms.controller;

import com.cms.dto.ApiResponse;
import com.cms.dto.auth.TokenResponse;
import com.cms.service.AuthService;
import com.cms.service.GoogleOAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Google OAuth 2.0 Authorization Code Flow endpoints.
 *
 * GET  /api/v1/auth/google/initiate   → redirect to Google consent screen
 * GET  /api/v1/auth/google/callback   → exchange code, issue JWT, redirect to frontend
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth/google")
@RequiredArgsConstructor
public class GoogleOAuthController {

    private final GoogleOAuthService googleOAuthService;

    /** Where to redirect the browser after a successful OAuth login */
    @Value("${google.oauth.frontend-redirect-uri:http://localhost:5173/oauth/callback}")
    private String frontendRedirectUri;

    /**
     * Step 1 – Initiates the OAuth flow.
     *
     * The client can either:
     *   a) Call this endpoint and follow the 302 redirect directly (server-side redirect), or
     *   b) GET /api/v1/auth/google/authorize-url and handle the redirect in JS.
     *
     * Both approaches are supported below.
     */
    @GetMapping("/initiate")
    public void initiate(HttpServletResponse response) throws IOException {
        String authorizationUrl = googleOAuthService.buildAuthorizationUrl();
        response.sendRedirect(authorizationUrl);
    }

    /**
     * Returns the Google consent-screen URL as JSON so the frontend can
     * open it in a popup or handle the redirect itself.
     */
    @GetMapping("/authorize-url")
    public ResponseEntity<ApiResponse<Map<String, String>>> authorizeUrl() {
        String url = googleOAuthService.buildAuthorizationUrl();
        return ResponseEntity.ok(ApiResponse.ok(Map.of("url", url)));
    }

    /**
     * Step 2 – Google redirects here with ?code=…&state=…
     *
     * After exchanging the code, the user is redirected to the frontend with
     * the CMS JWT tokens embedded as URL fragments so localStorage can pick them up.
     *
     * Fragment form: #access_token=…&refresh_token=…&expires_in=…
     */
    @GetMapping("/callback")
    public void callback(
            @RequestParam(value = "code",  required = false) String code,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "error", required = false) String error,
            HttpServletRequest  request,
            HttpServletResponse response) throws IOException {

        // Handle user-denied or error cases
        if (error != null || code == null) {
            String desc = error != null ? error : "missing_code";
            log.warn("Google OAuth callback error: {}", desc);
            response.sendRedirect(frontendRedirectUri +
                "?error=" + URLEncoder.encode(desc, StandardCharsets.UTF_8));
            return;
        }

        try {
            AuthService.AuthResult result =
                googleOAuthService.handleCallback(code, state, request.getRemoteAddr());

            TokenResponse tokenResponse = TokenResponse.from(result);

            // Build the redirect URL with tokens in the fragment (never in query params)
            String redirect = frontendRedirectUri
                + "?access_token="  + URLEncoder.encode(tokenResponse.getAccessToken(), StandardCharsets.UTF_8)
                + "&refresh_token=" + URLEncoder.encode(tokenResponse.getRefreshToken(), StandardCharsets.UTF_8)
                + "&expires_in="    + tokenResponse.getExpiresIn()
                + "&user_id="       + URLEncoder.encode(tokenResponse.getUser().getId(), StandardCharsets.UTF_8)
                + "&email="         + URLEncoder.encode(tokenResponse.getUser().getEmail(), StandardCharsets.UTF_8)
                + "&first_name="    + URLEncoder.encode(tokenResponse.getUser().getFirstName(), StandardCharsets.UTF_8)
                + "&last_name="     + URLEncoder.encode(tokenResponse.getUser().getLastName(), StandardCharsets.UTF_8)
                + "&org_id="        + URLEncoder.encode(tokenResponse.getUser().getOrganizationId(), StandardCharsets.UTF_8)
                + "&org_name="      + URLEncoder.encode(tokenResponse.getUser().getOrganizationName(), StandardCharsets.UTF_8);

            response.sendRedirect(redirect);

        } catch (com.cms.exception.AuthenticationException e) {
            log.warn("Google OAuth authentication failed: {} — {}", e.getCode(), e.getMessage());
            response.sendRedirect(frontendRedirectUri +
                "?error=" + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Unexpected error in Google OAuth callback", e);
            response.sendRedirect(frontendRedirectUri +
                "?error=" + URLEncoder.encode("internal_error", StandardCharsets.UTF_8));
        }
    }
}
