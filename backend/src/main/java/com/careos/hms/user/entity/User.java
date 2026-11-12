package com.careos.hms.user.entity;

import com.careos.hms.common.audit.SoftDeletableEntity;
import com.careos.hms.hospital.entity.Hospital;
import com.careos.hms.role.entity.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User extends SoftDeletableEntity {

    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    private String publicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id")
    private Hospital hospital;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_scope", nullable = false, length = 20)
    private UserScope userScope;

    @Column(name = "username", nullable = false, unique = true, length = 64)
    private String username;

    @Column(name = "email", nullable = false, unique = true, length = 160)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "first_name", nullable = false, length = 80)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 80)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts = 0;

    @Column(name = "last_failed_login_at")
    private Instant lastFailedLoginAt;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "password_changed_at")
    private Instant passwordChangedAt;

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword = false;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private Set<UserRole> userRoles = new LinkedHashSet<>();

    protected User() {
    }

    public User(
            Hospital hospital,
            UserScope userScope,
            String username,
            String email,
            String passwordHash,
            String firstName,
            String lastName
    ) {
        this.publicId = UUID.randomUUID().toString();
        this.hospital = hospital;
        this.userScope = userScope;
        this.username = normalizeIdentifier(username);
        this.email = normalizeIdentifier(email);
        this.passwordHash = passwordHash;
        this.firstName = firstName != null ? firstName.trim() : "";
        this.lastName = lastName != null ? lastName.trim() : "";
        this.status = AccountStatus.ACTIVE;
        this.passwordChangedAt = Instant.now();
        validateTenantAssociation();
    }

    @PrePersist
    @PreUpdate
    void enforceInvariants() {
        if (this.publicId == null || this.publicId.isBlank()) {
            this.publicId = UUID.randomUUID().toString();
        }
        this.username = normalizeIdentifier(this.username);
        this.email = normalizeIdentifier(this.email);
        validateTenantAssociation();
    }

    private void validateTenantAssociation() {
        if (this.userScope == UserScope.PLATFORM && this.hospital != null) {
            throw new IllegalStateException("Platform-scoped users must not belong to a hospital tenant.");
        }
        if (this.userScope == UserScope.HOSPITAL && this.hospital == null) {
            throw new IllegalStateException("Hospital-scoped users must be associated with a hospital.");
        }
    }

    private String normalizeIdentifier(String value) {
        return value != null ? value.trim().toLowerCase(Locale.ROOT) : "";
    }

    public String getPublicId() {
        return publicId;
    }

    public Hospital getHospital() {
        return hospital;
    }

    public Long getHospitalId() {
        return hospital != null ? hospital.getId() : null;
    }

    public UserScope getUserScope() {
        return userScope;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFullName() {
        return (firstName + " " + lastName).trim();
    }

    public AccountStatus getStatus() {
        return status;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public Instant getLastFailedLoginAt() {
        return lastFailedLoginAt;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public Instant getPasswordChangedAt() {
        return passwordChangedAt;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public Set<UserRole> getUserRoles() {
        return userRoles;
    }

    public boolean isTemporarilyLocked(Instant now) {
        return status == AccountStatus.LOCKED || (lockedUntil != null && lockedUntil.isAfter(now));
    }

    public boolean isLoginAllowed(Instant now) {
        return !isDeleted()
                && status == AccountStatus.ACTIVE
                && (lockedUntil == null || !lockedUntil.isAfter(now));
    }

    public void recordFailedLogin(Instant now, int maxFailedAttempts, int lockoutDurationMinutes) {
        if (this.lockedUntil != null && !this.lockedUntil.isAfter(now)) {
            this.failedLoginAttempts = 0;
            this.lockedUntil = null;
        }
        this.failedLoginAttempts += 1;
        this.lastFailedLoginAt = now;
        if (this.failedLoginAttempts >= maxFailedAttempts) {
            this.lockedUntil = now.plusSeconds(lockoutDurationMinutes * 60L);
        }
    }

    public void recordSuccessfulLogin(Instant now) {
        this.failedLoginAttempts = 0;
        this.lastFailedLoginAt = null;
        this.lockedUntil = null;
        this.lastLoginAt = now;
    }

    public void updatePasswordHash(String encodedPasswordHash, Instant changedAt) {
        this.passwordHash = encodedPasswordHash;
        this.passwordChangedAt = changedAt;
        this.mustChangePassword = false;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public void setMustChangePassword(boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }
}
