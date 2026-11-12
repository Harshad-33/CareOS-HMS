package com.careos.hms.authentication.service;

import com.careos.hms.security.CareOsSecurityProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

@Service
public class CookieService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private final CareOsSecurityProperties securityProperties;

    public CookieService(CareOsSecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    public void writeRefreshTokenCookie(
            HttpServletResponse response,
            String rawRefreshToken,
            long maxAgeSeconds,
            boolean persistent
    ) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(
                        securityProperties.getCookie().getRefreshTokenName(),
                        rawRefreshToken
                )
                .httpOnly(true)
                .secure(securityProperties.getCookie().isSecure())
                .path(securityProperties.getCookie().getPath())
                .sameSite(securityProperties.getCookie().getSameSite());

        applyOptionalDomain(builder);
        if (persistent) {
            builder.maxAge(maxAgeSeconds);
        }
        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }

    public String generateAndWriteCsrfCookie(HttpServletResponse response) {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        String csrfToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(
                        securityProperties.getCookie().getCsrfTokenName(),
                        csrfToken
                )
                .httpOnly(true)
                .secure(securityProperties.getCookie().isSecure())
                .path(securityProperties.getCookie().getPath())
                .sameSite(securityProperties.getCookie().getSameSite());

        applyOptionalDomain(builder);
        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
        return csrfToken;
    }

    public void clearAuthCookies(HttpServletResponse response) {
        clearCookie(response, securityProperties.getCookie().getRefreshTokenName());
        clearCookie(response, securityProperties.getCookie().getCsrfTokenName());
    }

    public Optional<String> extractRefreshTokenCookie(HttpServletRequest request) {
        return extractCookie(request, securityProperties.getCookie().getRefreshTokenName());
    }

    public Optional<String> extractCsrfCookie(HttpServletRequest request) {
        return extractCookie(request, securityProperties.getCookie().getCsrfTokenName());
    }

    private Optional<String> extractCookie(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                return Optional.of(cookie.getValue().trim());
            }
        }
        return Optional.empty();
    }

    private void clearCookie(HttpServletResponse response, String cookieName) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(securityProperties.getCookie().isSecure())
                .path(securityProperties.getCookie().getPath())
                .sameSite(securityProperties.getCookie().getSameSite())
                .maxAge(0);

        applyOptionalDomain(builder);
        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }

    private void applyOptionalDomain(ResponseCookie.ResponseCookieBuilder builder) {
        String domain = securityProperties.getCookie().getDomain();
        if (domain != null && !domain.isBlank()) {
            builder.domain(domain.trim());
        }
    }
}
