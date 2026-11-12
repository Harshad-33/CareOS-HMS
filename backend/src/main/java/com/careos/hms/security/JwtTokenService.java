package com.careos.hms.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class JwtTokenService {

    public static final String TOKEN_TYPE_CLAIM = "token_type";
    public static final String TOKEN_TYPE_ACCESS = "ACCESS";
    public static final String CLAIM_USERNAME = "username";
    public static final String CLAIM_SCOPE = "scope";
    public static final String CLAIM_HOSPITAL_ID = "hospital_id";
    public static final String CLAIM_ROLES = "roles";
    public static final String CLAIM_PERMISSIONS = "permissions";
    public static final String CLAIM_PWD_TS = "pwd_ts";

    private final CareOsSecurityProperties properties;
    private final SecretKey signingKey;

    public JwtTokenService(CareOsSecurityProperties properties) {
        this.properties = properties;
        this.signingKey = Keys.hmacShaKeyFor(properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(CareOsUserPrincipal principal) {
        return generateAccessToken(principal, Instant.now());
    }

    public String generateAccessToken(CareOsUserPrincipal principal, Instant issuedAt) {
        Instant expiresAt = issuedAt.plusSeconds(properties.getJwt().getAccessTokenExpirationSeconds());

        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put(TOKEN_TYPE_CLAIM, TOKEN_TYPE_ACCESS);
        claims.put(CLAIM_USERNAME, principal.getUsername());
        claims.put(CLAIM_SCOPE, principal.getUserScope().name());
        if (principal.getHospitalId() != null) {
            claims.put(CLAIM_HOSPITAL_ID, principal.getHospitalId());
        }
        claims.put(CLAIM_ROLES, principal.getRoles());
        claims.put(CLAIM_PERMISSIONS, principal.getPermissions());
        claims.put(
                CLAIM_PWD_TS,
                principal.getPasswordChangedAt() != null ? principal.getPasswordChangedAt().toEpochMilli() : 0L
        );

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(properties.getJwt().getIssuer())
                .subject(principal.getPublicId())
                .claims(claims)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public AccessTokenClaims parseAndValidateAccessToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(properties.getJwt().getIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);
        if (!TOKEN_TYPE_ACCESS.equals(tokenType)) {
            throw new JwtException("Invalid token type");
        }
        String publicId = claims.getSubject();
        if (publicId == null || publicId.isBlank()) {
            throw new JwtException("Token subject is missing");
        }
        Number pwdTsNumber = claims.get(CLAIM_PWD_TS, Number.class);
        long pwdTs = pwdTsNumber != null ? pwdTsNumber.longValue() : 0L;
        Instant issuedAt = claims.getIssuedAt() != null ? claims.getIssuedAt().toInstant() : Instant.EPOCH;

        return new AccessTokenClaims(publicId, claims.getId(), issuedAt, pwdTs);
    }

    public boolean isTokenExpiredException(JwtException ex) {
        return ex instanceof ExpiredJwtException;
    }

    public record AccessTokenClaims(
            String publicId,
            String jti,
            Instant issuedAt,
            long passwordChangedEpochMillis
    ) {
    }
}
