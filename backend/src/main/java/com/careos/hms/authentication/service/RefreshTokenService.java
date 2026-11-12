package com.careos.hms.authentication.service;

import com.careos.hms.authentication.entity.RefreshToken;
import com.careos.hms.authentication.entity.TokenRevocationReason;
import com.careos.hms.authentication.repository.RefreshTokenRepository;
import com.careos.hms.common.exception.AccountLockedException;
import com.careos.hms.common.exception.CareOsException;
import com.careos.hms.common.exception.UnauthorizedException;
import com.careos.hms.hospital.entity.HospitalStatus;
import com.careos.hms.security.CareOsSecurityProperties;
import com.careos.hms.user.entity.AccountStatus;
import com.careos.hms.user.entity.User;
import com.careos.hms.user.entity.UserScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final RefreshTokenRepository refreshTokenRepository;
    private final CareOsSecurityProperties securityProperties;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            CareOsSecurityProperties securityProperties
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.securityProperties = securityProperties;
    }

    @Transactional
    public IssuedRefreshToken issueNewFamilyToken(
            User user,
            boolean rememberMe,
            String clientIp,
            String userAgent
    ) {
        String familyId = UUID.randomUUID().toString();
        return createTokenInFamily(user, familyId, rememberMe, clientIp, userAgent, Instant.now());
    }

    @Transactional(noRollbackFor = CareOsException.class)
    public RotatedRefreshToken rotateToken(
            String rawRefreshToken,
            String clientIp,
            String userAgent
    ) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new UnauthorizedException("MISSING_REFRESH_TOKEN", "Refresh token cookie is required");
        }

        Instant now = Instant.now();
        String tokenHash = hashToken(rawRefreshToken.trim());
        RefreshToken existing = refreshTokenRepository.findByTokenHashWithUser(tokenHash)
                .orElseThrow(() -> new UnauthorizedException("INVALID_REFRESH_TOKEN", "Refresh session is invalid"));

        User user = existing.getUser();

        if (existing.isRevoked()) {
            // Reuse of an already-rotated/revoked refresh token compromises the entire token family.
            refreshTokenRepository.revokeActiveTokensInFamily(
                    existing.getFamilyId(),
                    TokenRevocationReason.REUSE_DETECTED,
                    now
            );
            log.warn(
                    "Refresh token reuse detected: userPublicId={}, familyId={}, tokenId={}",
                    user.getPublicId(),
                    existing.getFamilyId(),
                    existing.getTokenId()
            );
            throw new UnauthorizedException(
                    "REFRESH_TOKEN_REUSE_DETECTED",
                    "Session security violation detected. Please sign in again."
            );
        }

        if (existing.isExpired(now)) {
            existing.revoke(TokenRevocationReason.EXPIRED, now, null);
            refreshTokenRepository.save(existing);
            throw new UnauthorizedException("REFRESH_TOKEN_EXPIRED", "Session has expired. Please sign in again.");
        }

        if (user.isTemporarilyLocked(now)) {
            refreshTokenRepository.revokeActiveTokensInFamily(
                    existing.getFamilyId(),
                    TokenRevocationReason.ACCOUNT_DISABLED,
                    now
            );
            throw new AccountLockedException("Account is temporarily locked. Please try again later.");
        }

        if (user.isDeleted()
                || user.getStatus() != AccountStatus.ACTIVE
                || (user.getUserScope() == UserScope.HOSPITAL
                && (user.getHospital() == null
                || user.getHospital().isDeleted()
                || user.getHospital().getStatus() != HospitalStatus.ACTIVE))) {
            refreshTokenRepository.revokeActiveTokensInFamily(
                    existing.getFamilyId(),
                    TokenRevocationReason.ACCOUNT_DISABLED,
                    now
            );
            throw new UnauthorizedException("ACCOUNT_UNAVAILABLE", "Account is unavailable. Please contact an administrator.");
        }

        IssuedRefreshToken nextToken = createTokenInFamily(
                user,
                existing.getFamilyId(),
                existing.isRememberMe(),
                clientIp,
                userAgent,
                now
        );

        existing.revoke(TokenRevocationReason.ROTATED, now, nextToken.tokenId());
        refreshTokenRepository.save(existing);

        return new RotatedRefreshToken(user.getPublicId(), nextToken);
    }

    @Transactional
    public void revokeByRawTokenIfPresent(String rawRefreshToken, TokenRevocationReason reason) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        String tokenHash = hashToken(rawRefreshToken.trim());
        refreshTokenRepository.findByTokenHashWithUser(tokenHash).ifPresent(token -> {
            if (!token.isRevoked()) {
                token.revoke(reason, Instant.now(), null);
                refreshTokenRepository.save(token);
            }
        });
    }

    @Transactional
    public int revokeAllForUser(Long userId, TokenRevocationReason reason) {
        return refreshTokenRepository.revokeAllActiveTokensForUser(userId, reason, Instant.now());
    }

    public String hashToken(String rawToken) {
        try {
            String pepper = securityProperties.getRefreshToken().getHmacSecret();
            if (pepper == null || pepper.isBlank()) {
                pepper = securityProperties.getJwt().getSecret();
            }
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(pepper.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] digest = mac.doFinal(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Unable to hash refresh token", ex);
        }
    }

    private IssuedRefreshToken createTokenInFamily(
            User user,
            String familyId,
            boolean rememberMe,
            String clientIp,
            String userAgent,
            Instant now
    ) {
        String tokenId = UUID.randomUUID().toString();
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        String rawSecret = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        String rawToken = tokenId + "." + rawSecret;
        String tokenHash = hashToken(rawToken);

        long ttlSeconds = rememberMe
                ? securityProperties.getRefreshToken().getRememberMeExpirationSeconds()
                : securityProperties.getRefreshToken().getSessionExpirationSeconds();
        Instant expiresAt = now.plusSeconds(ttlSeconds);

        RefreshToken entity = new RefreshToken(
                tokenId,
                familyId,
                user,
                tokenHash,
                expiresAt,
                rememberMe,
                clientIp,
                userAgent
        );
        refreshTokenRepository.save(entity);
        return new IssuedRefreshToken(tokenId, familyId, rawToken, ttlSeconds, rememberMe);
    }

    public record IssuedRefreshToken(
            String tokenId,
            String familyId,
            String rawToken,
            long ttlSeconds,
            boolean rememberMe
    ) {
    }

    public record RotatedRefreshToken(
            String userPublicId,
            IssuedRefreshToken issuedToken
    ) {
    }
}
