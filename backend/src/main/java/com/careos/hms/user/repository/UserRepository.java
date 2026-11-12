package com.careos.hms.user.repository;

import com.careos.hms.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query("""
            SELECT DISTINCT u FROM User u
            LEFT JOIN FETCH u.hospital h
            LEFT JOIN FETCH u.userRoles ur
            LEFT JOIN FETCH ur.role r
            LEFT JOIN FETCH r.rolePermissions rp
            LEFT JOIN FETCH rp.permission p
            WHERE (LOWER(u.username) = LOWER(:identifier) OR LOWER(u.email) = LOWER(:identifier))
              AND u.deleted = false
            """)
    Optional<User> findActiveGraphByUsernameOrEmail(@Param("identifier") String identifier);

    @Query("""
            SELECT DISTINCT u FROM User u
            LEFT JOIN FETCH u.hospital h
            LEFT JOIN FETCH u.userRoles ur
            LEFT JOIN FETCH ur.role r
            LEFT JOIN FETCH r.rolePermissions rp
            LEFT JOIN FETCH rp.permission p
            WHERE u.publicId = :publicId
              AND u.deleted = false
            """)
    Optional<User> findActiveGraphByPublicId(@Param("publicId") String publicId);

    @Query("""
            SELECT CASE WHEN COUNT(ur) > 0 THEN true ELSE false END
            FROM UserRole ur
            JOIN ur.user u
            JOIN ur.role r
            WHERE r.code = :roleCode
              AND u.deleted = false
            """)
    boolean existsByRoleCode(@Param("roleCode") String roleCode);

    boolean existsByUsernameIgnoreCaseOrEmailIgnoreCase(String username, String email);

    @Query("""
            SELECT u FROM User u
            LEFT JOIN FETCH u.hospital h
            WHERE u.id = :id
              AND h.id = :hospitalId
              AND u.deleted = false
            """)
    Optional<User> findByIdAndHospitalIdAndDeletedFalse(
            @Param("id") Long id,
            @Param("hospitalId") Long hospitalId
    );

    @Query("""
            SELECT u FROM User u
            WHERE u.hospital.id = :hospitalId
              AND u.deleted = false
            """)
    List<User> findAllByHospitalIdAndDeletedFalse(@Param("hospitalId") Long hospitalId);
}
