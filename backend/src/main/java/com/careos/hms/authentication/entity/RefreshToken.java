package com.careos.hms.authentication.entity;

import com.careos.hms.common.audit.BaseEntity;
import com.careos.hms.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken extends BaseEntity {

    @Column(name = "token_id", nullable = false, unique = true, length = 36)
    private String tokenId;

    @Column(name = "family_id", nullable = false, length = 36)
    private String familyId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "remember_me", nullable = false)
    private boolean rememberMe = false;

    @Column(name = "is_revoked", nullable = false)
    private boolean revoked = false;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "revoked_reason", length = 64)
    private TokenRevocationReason revokedReason;

    @Column(name = "replaced_by_token_id", length = 36)
    private String replacedByTokenId;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "client_ip", length = 64)
    private String clientIp;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    protected RefreshToken() {
    }

    public RefreshToken(
            String tokenId,
            String familyId,
            User user,
            String tokenHash,
            Instant expiresAt,
            boolean rememberMe,
            String clientIp,
            String userAgent
    ) {
        this.tokenId = tokenId;
        this.familyId = familyId;
        this.user = user;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.rememberMe = rememberMe;
        this.revoked = false;
        this.clientIp = truncate(clientIp, 64);
        this.userAgent = truncate(userAgent, 255);
    }

    private String truncate(String value, int maxLen) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLen ? trimmed : trimmed.substring(0, maxLen);
    }

    public String getTokenId() {
        return tokenId;
    }

    public String getFamilyId() {
        return familyId;
    }

    public User getUser() {
        return user;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isRememberMe() {
        return rememberMe;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public TokenRevocationReason getRevokedReason() {
        return revokedReason;
    }

    public String getReplacedByTokenId() {
        return replacedByTokenId;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }

    public void revoke(TokenRevocationReason reason, Instant now, String replacedByTokenId) {
        this.revoked = true;
        this.revokedReason = reason;
        this.revokedAt = now;
        this.lastUsedAt = now;
        this.replacedByTokenId = replacedByTokenId;
    }
}
