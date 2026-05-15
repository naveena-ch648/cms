package com.cms.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final CustomUserDetailsService userDetailsService;
    private final JdbcTemplate pgJdbc;

    private static final String BLOCKLIST_PREFIX = "jwt:blocklist:";

    public JwtAuthenticationFilter(JwtProvider jwtProvider,
                                    CustomUserDetailsService userDetailsService,
                                    @Qualifier("pgJdbcTemplate") JdbcTemplate pgJdbc) {
        this.jwtProvider = jwtProvider;
        this.userDetailsService = userDetailsService;
        this.pgJdbc = pgJdbc;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        if (token != null && jwtProvider.validateToken(token)) {
            String jti = jwtProvider.getTokenId(token);

            // Check PostgreSQL blocklist
            try {
                Integer count = pgJdbc.queryForObject(
                    "SELECT COUNT(*) FROM jwt_tokens WHERE jti=? AND token_type='BLOCKLIST' AND expires_at>NOW()",
                    Integer.class, BLOCKLIST_PREFIX + jti);
                if (count != null && count > 0) {
                    filterChain.doFilter(request, response);
                    return;
                }
            } catch (Exception e) {
                log.warn("PG unavailable — JWT blocklist check skipped: {}", e.getMessage());
            }

            Long userId = jwtProvider.getUserId(token);
            UserDetails userDetails = userDetailsService.loadUserById(userId);

            if (userDetails != null) {
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
