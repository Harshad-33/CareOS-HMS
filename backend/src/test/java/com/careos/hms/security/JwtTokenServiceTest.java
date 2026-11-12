package com.careos.hms.security;

import com.careos.hms.hospital.entity.Hospital;
import com.careos.hms.permission.entity.PermissionCode;
import com.careos.hms.role.entity.SystemRoleCode;
import com.careos.hms.user.entity.AccountStatus;
import com.careos.hms.user.entity.User;
import com.careos.hms.user.entity.UserScope;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenServiceTest {

    private static final String TEST_SECRET = "test-only-careos-hms-jwt-signing-key-minimum-32-bytes-long";

    private CareOsSecurityProperties properties;
    private JwtTokenService jwtTokenService;

    @BeforeEach
    void setUp() {
        properties = new CareOsSecurityProperties();
        properties.getJwt().setSecret(TEST_SECRET);
        properties.getJwt().setIssuer("careos-hms-test");
        properties.getJwt().setAccessTokenExpirationSeconds(900);
        jwtTokenService = new JwtTokenService(properties);
    }

    @Test
    void shouldGenerateAndValidateAccessTokenWithExpectedClaims() {
        Hospital hospital = new Hospital("HOSP-001", "CareOS General");
        ReflectionTestUtils.setField(hospital, "id", 42L);
        User user = new User(
                hospital,
                UserScope.HOSPITAL,
                "admin.careos",
                "admin@careos.local",
                "$2a$04$hashed",
                "CareOS",
                "Admin"
        );
        ReflectionTestUtils.setField(user, "id", 7L);

        CareOsUserPrincipal principal = new CareOsUserPrincipal(
                user.getId(),
                user.getPublicId(),
                user.getUsername(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getFirstName(),
                user.getLastName(),
                user.getUserScope(),
                hospital.getId(),
                hospital.getHospitalCode(),
                hospital.getName(),
                AccountStatus.ACTIVE,
                false,
                false,
                null,
                null,
                user.getPasswordChangedAt(),
                Set.of(SystemRoleCode.HOSPITAL_ADMIN),
                Set.of(PermissionCode.HOSPITAL_WORKSPACE_READ, PermissionCode.AUTH_ME_READ)
        );

        Instant now = Instant.now();
        String token = jwtTokenService.generateAccessToken(principal, now);
        JwtTokenService.AccessTokenClaims claims = jwtTokenService.parseAndValidateAccessToken(token);

        assertThat(claims.publicId()).isEqualTo(user.getPublicId());
        assertThat(claims.jti()).isNotBlank();
        assertThat(claims.passwordChangedEpochMillis()).isEqualTo(user.getPasswordChangedAt().toEpochMilli());

        var rawClaims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(rawClaims.get(JwtTokenService.CLAIM_SCOPE, String.class)).isEqualTo("HOSPITAL");
        assertThat(rawClaims.get(JwtTokenService.CLAIM_HOSPITAL_ID, Number.class).longValue()).isEqualTo(42L);
        assertThat(rawClaims.get(JwtTokenService.CLAIM_USERNAME, String.class)).isEqualTo("admin.careos");
    }

    @Test
    void shouldRejectExpiredAccessToken() {
        User user = new User(
                null,
                UserScope.PLATFORM,
                "super.admin",
                "super@careos.local",
                "$2a$04$hashed",
                "Platform",
                "Admin"
        );
        CareOsUserPrincipal principal = new CareOsUserPrincipal(
                1L,
                user.getPublicId(),
                user.getUsername(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getFirstName(),
                user.getLastName(),
                user.getUserScope(),
                null,
                null,
                null,
                AccountStatus.ACTIVE,
                false,
                false,
                null,
                null,
                user.getPasswordChangedAt(),
                Set.of(SystemRoleCode.PLATFORM_SUPER_ADMIN),
                Set.of(PermissionCode.PLATFORM_HOSPITALS_MANAGE)
        );

        String expiredToken = jwtTokenService.generateAccessToken(principal, Instant.now().minusSeconds(3600));

        assertThatThrownBy(() -> jwtTokenService.parseAndValidateAccessToken(expiredToken))
                .isInstanceOf(JwtException.class)
                .satisfies(ex -> assertThat(jwtTokenService.isTokenExpiredException((JwtException) ex)).isTrue());
    }

    @Test
    void shouldRejectTamperedSignatureAndInvalidTokenType() {
        String wrongKeyToken = Jwts.builder()
                .issuer("careos-hms-test")
                .subject("user-123")
                .claim(JwtTokenService.TOKEN_TYPE_CLAIM, JwtTokenService.TOKEN_TYPE_ACCESS)
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(Keys.hmacShaKeyFor("different-signing-key-that-must-be-rejected-123456".getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThatThrownBy(() -> jwtTokenService.parseAndValidateAccessToken(wrongKeyToken))
                .isInstanceOf(JwtException.class);

        String wrongTypeToken = Jwts.builder()
                .issuer("careos-hms-test")
                .subject("user-123")
                .claim(JwtTokenService.TOKEN_TYPE_CLAIM, "REFRESH")
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThatThrownBy(() -> jwtTokenService.parseAndValidateAccessToken(wrongTypeToken))
                .isInstanceOf(JwtException.class);
    }
}
