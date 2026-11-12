package com.careos.hms.security;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@ConfigurationProperties(prefix = "careos.security")
public class CareOsSecurityProperties {

    private final JwtProperties jwt = new JwtProperties();
    private final RefreshTokenProperties refreshToken = new RefreshTokenProperties();
    private final CookieProperties cookie = new CookieProperties();
    private final LoginProperties login = new LoginProperties();
    private final BootstrapProperties bootstrap = new BootstrapProperties();

    @PostConstruct
    void validateConfiguration() {
        if (jwt.getSecret() == null || jwt.getSecret().isBlank()) {
            throw new IllegalStateException(
                    "careos.security.jwt.secret (JWT_SECRET) must be configured with at least 32 bytes."
            );
        }
        if (jwt.getSecret().getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "careos.security.jwt.secret (JWT_SECRET) must be at least 32 bytes for HS256 signing."
            );
        }
        if (jwt.getAccessTokenExpirationSeconds() < 60 || jwt.getAccessTokenExpirationSeconds() > 3600) {
            throw new IllegalStateException(
                    "careos.security.jwt.access-token-expiration-seconds should be between 60 and 3600 seconds."
            );
        }
    }

    public JwtProperties getJwt() {
        return jwt;
    }

    public RefreshTokenProperties getRefreshToken() {
        return refreshToken;
    }

    public CookieProperties getCookie() {
        return cookie;
    }

    public LoginProperties getLogin() {
        return login;
    }

    public BootstrapProperties getBootstrap() {
        return bootstrap;
    }

    public static class JwtProperties {
        private String secret;
        private String issuer = "careos-hms";
        private long accessTokenExpirationSeconds = 900;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        public long getAccessTokenExpirationSeconds() {
            return accessTokenExpirationSeconds;
        }

        public void setAccessTokenExpirationSeconds(long accessTokenExpirationSeconds) {
            this.accessTokenExpirationSeconds = accessTokenExpirationSeconds;
        }
    }

    public static class RefreshTokenProperties {
        private String hmacSecret;
        private long sessionExpirationSeconds = 43200;
        private long rememberMeExpirationSeconds = 604800;

        public String getHmacSecret() {
            return hmacSecret;
        }

        public void setHmacSecret(String hmacSecret) {
            this.hmacSecret = hmacSecret;
        }

        public long getSessionExpirationSeconds() {
            return sessionExpirationSeconds;
        }

        public void setSessionExpirationSeconds(long sessionExpirationSeconds) {
            this.sessionExpirationSeconds = sessionExpirationSeconds;
        }

        public long getRememberMeExpirationSeconds() {
            return rememberMeExpirationSeconds;
        }

        public void setRememberMeExpirationSeconds(long rememberMeExpirationSeconds) {
            this.rememberMeExpirationSeconds = rememberMeExpirationSeconds;
        }
    }

    public static class CookieProperties {
        private String refreshTokenName = "careos_refresh_token";
        private String csrfTokenName = "careos_csrf_token";
        private String path = "/api/v1/auth";
        private String domain = "";
        private boolean secure = false;
        private String sameSite = "Lax";

        public String getRefreshTokenName() {
            return refreshTokenName;
        }

        public void setRefreshTokenName(String refreshTokenName) {
            this.refreshTokenName = refreshTokenName;
        }

        public String getCsrfTokenName() {
            return csrfTokenName;
        }

        public void setCsrfTokenName(String csrfTokenName) {
            this.csrfTokenName = csrfTokenName;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getDomain() {
            return domain;
        }

        public void setDomain(String domain) {
            this.domain = domain;
        }

        public boolean isSecure() {
            return secure;
        }

        public void setSecure(boolean secure) {
            this.secure = secure;
        }

        public String getSameSite() {
            return sameSite;
        }

        public void setSameSite(String sameSite) {
            this.sameSite = sameSite;
        }
    }

    public static class LoginProperties {
        private int bcryptStrength = 12;
        private int maxFailedAttempts = 5;
        private int lockoutDurationMinutes = 15;
        private int rateLimitMaxAttempts = 20;
        private int rateLimitWindowSeconds = 60;

        public int getBcryptStrength() {
            return bcryptStrength;
        }

        public void setBcryptStrength(int bcryptStrength) {
            this.bcryptStrength = bcryptStrength;
        }

        public int getMaxFailedAttempts() {
            return maxFailedAttempts;
        }

        public void setMaxFailedAttempts(int maxFailedAttempts) {
            this.maxFailedAttempts = maxFailedAttempts;
        }

        public int getLockoutDurationMinutes() {
            return lockoutDurationMinutes;
        }

        public void setLockoutDurationMinutes(int lockoutDurationMinutes) {
            this.lockoutDurationMinutes = lockoutDurationMinutes;
        }

        public int getRateLimitMaxAttempts() {
            return rateLimitMaxAttempts;
        }

        public void setRateLimitMaxAttempts(int rateLimitMaxAttempts) {
            this.rateLimitMaxAttempts = rateLimitMaxAttempts;
        }

        public int getRateLimitWindowSeconds() {
            return rateLimitWindowSeconds;
        }

        public void setRateLimitWindowSeconds(int rateLimitWindowSeconds) {
            this.rateLimitWindowSeconds = rateLimitWindowSeconds;
        }
    }

    public static class BootstrapProperties {
        private boolean enabled = false;
        private String username;
        private String email;
        private String password;
        private String firstName = "Platform";
        private String lastName = "Administrator";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }
    }
}
