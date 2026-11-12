package com.careos.hms.security;

import com.careos.hms.common.constants.ApiConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Enforces CSRF protection (double-submit cookie + custom header + Origin verification)
 * for cookie-authenticated refresh and logout operations.
 */
@Component
public class AuthCookieCsrfFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AuthCookieCsrfFilter.class);
    private static final String REFRESH_PATH = ApiConstants.API_V1_BASE_PATH + "/auth/refresh";
    private static final String LOGOUT_PATH = ApiConstants.API_V1_BASE_PATH + "/auth/logout";

    private final CareOsSecurityProperties securityProperties;
    private final CareOsAccessDeniedHandler accessDeniedHandler;
    private final Set<String> allowedOrigins;

    public AuthCookieCsrfFilter(
            CareOsSecurityProperties securityProperties,
            CareOsAccessDeniedHandler accessDeniedHandler,
            @Value("${app.cors.allowed-origins:http://localhost:4200,http://localhost:4201}") String[] allowedOrigins
    ) {
        this.securityProperties = securityProperties;
        this.accessDeniedHandler = accessDeniedHandler;
        this.allowedOrigins = Arrays.stream(allowedOrigins)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!requiresCsrfCheck(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!isAllowedOrigin(request)) {
            log.warn("CSRF origin mismatch blocked at {}", request.getRequestURI());
            accessDeniedHandler.writeForbiddenResponse(
                    response,
                    request,
                    "CSRF_ORIGIN_DENIED",
                    "Cross-origin request origin is not permitted"
            );
            return;
        }

        String headerToken = request.getHeader(ApiConstants.CSRF_TOKEN_HEADER);
        String cookieToken = findCookieValue(request, securityProperties.getCookie().getCsrfTokenName());

        if (headerToken == null || headerToken.isBlank() || cookieToken == null || cookieToken.isBlank()) {
            log.warn("Missing CSRF token for cookie-authenticated endpoint {}", request.getRequestURI());
            accessDeniedHandler.writeForbiddenResponse(
                    response,
                    request,
                    "CSRF_TOKEN_INVALID",
                    "Missing or invalid CSRF protection token"
            );
            return;
        }

        boolean matches = MessageDigest.isEqual(
                headerToken.trim().getBytes(StandardCharsets.UTF_8),
                cookieToken.trim().getBytes(StandardCharsets.UTF_8)
        );
        if (!matches) {
            log.warn("CSRF token mismatch blocked at {}", request.getRequestURI());
            accessDeniedHandler.writeForbiddenResponse(
                    response,
                    request,
                    "CSRF_TOKEN_INVALID",
                    "Missing or invalid CSRF protection token"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean requiresCsrfCheck(HttpServletRequest request) {
        if (!HttpMethod.POST.matches(request.getMethod())) {
            return false;
        }
        String uri = request.getRequestURI();
        if (REFRESH_PATH.equals(uri)) {
            return true;
        }
        if (LOGOUT_PATH.equals(uri)) {
            return findCookieValue(request, securityProperties.getCookie().getRefreshTokenName()) != null;
        }
        return false;
    }

    private boolean isAllowedOrigin(HttpServletRequest request) {
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (origin != null && !origin.isBlank()) {
            return allowedOrigins.contains(origin.trim());
        }
        String referer = request.getHeader(HttpHeaders.REFERER);
        if (referer != null && !referer.isBlank()) {
            try {
                URI uri = URI.create(referer.trim());
                String reconstructed = uri.getScheme() + "://" + uri.getAuthority();
                return allowedOrigins.contains(reconstructed);
            } catch (IllegalArgumentException ex) {
                return false;
            }
        }
        // Non-browser API clients / server-to-server requests without Origin/Referer still verify double-submit CSRF.
        return true;
    }

    private String findCookieValue(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
