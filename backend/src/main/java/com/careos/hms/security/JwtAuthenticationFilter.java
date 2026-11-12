package com.careos.hms.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenService jwtTokenService;
    private final CareOsUserDetailsService userDetailsService;
    private final CareOsAuthenticationEntryPoint authenticationEntryPoint;

    public JwtAuthenticationFilter(
            JwtTokenService jwtTokenService,
            CareOsUserDetailsService userDetailsService,
            CareOsAuthenticationEntryPoint authenticationEntryPoint
    ) {
        this.jwtTokenService = jwtTokenService;
        this.userDetailsService = userDetailsService;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            rejectInvalidToken(request, response, "INVALID_TOKEN", "Bearer access token is missing");
            return;
        }

        try {
            JwtTokenService.AccessTokenClaims claims = jwtTokenService.parseAndValidateAccessToken(token);
            CareOsUserPrincipal principal = userDetailsService.loadPrincipalByPublicId(claims.publicId());

            if (!principal.isEnabled() || !principal.isAccountNonLocked()) {
                rejectInvalidToken(request, response, "ACCOUNT_UNAVAILABLE", "Account is inactive or locked");
                return;
            }

            if (isIssuedBeforePasswordChange(claims, principal)) {
                rejectInvalidToken(request, response, "TOKEN_REVOKED", "Session credentials have changed; please sign in again");
                return;
            }

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    principal.getAuthorities()
            );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);
        } catch (ExpiredJwtException ex) {
            log.debug("Expired access token rejected at {}", request.getRequestURI());
            rejectInvalidToken(request, response, "TOKEN_EXPIRED", "Access token has expired");
        } catch (JwtException | UsernameNotFoundException | IllegalArgumentException ex) {
            log.warn("Invalid access token rejected at {}: {}", request.getRequestURI(), ex.getClass().getSimpleName());
            rejectInvalidToken(request, response, "INVALID_TOKEN", "Access token is invalid");
        }
    }

    private boolean isIssuedBeforePasswordChange(
            JwtTokenService.AccessTokenClaims claims,
            CareOsUserPrincipal principal
    ) {
        if (principal.getPasswordChangedAt() == null) {
            return false;
        }
        long currentPwdEpochMillis = principal.getPasswordChangedAt().toEpochMilli();
        return currentPwdEpochMillis > claims.passwordChangedEpochMillis();
    }

    private void rejectInvalidToken(
            HttpServletRequest request,
            HttpServletResponse response,
            String errorCode,
            String message
    ) throws IOException {
        SecurityContextHolder.clearContext();
        request.setAttribute(CareOsAuthenticationEntryPoint.AUTH_ERROR_CODE_ATTR, errorCode);
        request.setAttribute(CareOsAuthenticationEntryPoint.AUTH_ERROR_MESSAGE_ATTR, message);
        authenticationEntryPoint.commence(request, response, new BadCredentialsException(message));
    }
}
