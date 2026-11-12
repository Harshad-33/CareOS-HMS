package com.careos.hms.authentication.repository;

import com.careos.hms.authentication.entity.RefreshToken;
import com.careos.hms.authentication.entity.TokenRevocationReason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    @Query("""
            SELECT rt FROM RefreshToken rt
            JOIN FETCH rt.user u
            WHERE rt.tokenHash = :tokenHash
            """)
    Optional<RefreshToken> findByTokenHashWithUser(@Param("tokenHash") String tokenHash);

    List<RefreshToken> findAllByFamilyId(String familyId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE RefreshToken rt
            SET rt.revoked = true,
                rt.revokedAt = :revokedAt,
                rt.revokedReason = :reason
            WHERE rt.familyId = :familyId
              AND rt.revoked = false
            """)
    int revokeActiveTokensInFamily(
            @Param("familyId") String familyId,
            @Param("reason") TokenRevocationReason reason,
            @Param("revokedAt") Instant revokedAt
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE RefreshToken rt
            SET rt.revoked = true,
                rt.revokedAt = :revokedAt,
                rt.revokedReason = :reason
            WHERE rt.user.id = :userId
              AND rt.revoked = false
            """)
    int revokeAllActiveTokensForUser(
            @Param("userId") Long userId,
            @Param("reason") TokenRevocationReason reason,
            @Param("revokedAt") Instant revokedAt
    );
}
