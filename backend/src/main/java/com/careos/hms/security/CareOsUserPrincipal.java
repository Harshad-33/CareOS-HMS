package com.careos.hms.security;

import com.careos.hms.role.entity.Role;
import com.careos.hms.role.entity.UserRole;
import com.careos.hms.user.entity.AccountStatus;
import com.careos.hms.user.entity.User;
import com.careos.hms.user.entity.UserScope;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public class CareOsUserPrincipal implements UserDetails {

    private final Long userId;
    private final String publicId;
    private final String username;
    private final String email;
    private final String passwordHash;
    private final String firstName;
    private final String lastName;
    private final String fullName;
    private final UserScope userScope;
    private final Long hospitalId;
    private final String hospitalCode;
    private final String hospitalName;
    private final AccountStatus status;
    private final boolean deleted;
    private final boolean mustChangePassword;
    private final Instant lockedUntil;
    private final Instant lastLoginAt;
    private final Instant passwordChangedAt;
    private final Set<String> roles;
    private final Set<String> permissions;
    private final Set<GrantedAuthority> authorities;

    public CareOsUserPrincipal(
            Long userId,
            String publicId,
            String username,
            String email,
            String passwordHash,
            String firstName,
            String lastName,
            UserScope userScope,
            Long hospitalId,
            String hospitalCode,
            String hospitalName,
            AccountStatus status,
            boolean deleted,
            boolean mustChangePassword,
            Instant lockedUntil,
            Instant lastLoginAt,
            Instant passwordChangedAt,
            Set<String> roles,
            Set<String> permissions
    ) {
        this.userId = userId;
        this.publicId = publicId;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.firstName = firstName;
        this.lastName = lastName;
        this.fullName = (firstName + " " + lastName).trim();
        this.userScope = userScope;
        this.hospitalId = hospitalId;
        this.hospitalCode = hospitalCode;
        this.hospitalName = hospitalName;
        this.status = status;
        this.deleted = deleted;
        this.mustChangePassword = mustChangePassword;
        this.lockedUntil = lockedUntil;
        this.lastLoginAt = lastLoginAt;
        this.passwordChangedAt = passwordChangedAt;
        this.roles = Collections.unmodifiableSet(new LinkedHashSet<>(roles));
        this.permissions = Collections.unmodifiableSet(new LinkedHashSet<>(permissions));

        Set<GrantedAuthority> granted = new LinkedHashSet<>();
        for (String roleCode : this.roles) {
            granted.add(new SimpleGrantedAuthority("ROLE_" + roleCode));
        }
        for (String permissionCode : this.permissions) {
            granted.add(new SimpleGrantedAuthority(permissionCode));
        }
        this.authorities = Collections.unmodifiableSet(granted);
    }

    public static CareOsUserPrincipal fromUser(User user) {
        Set<String> roleCodes = new LinkedHashSet<>();
        Set<String> permissionCodes = new LinkedHashSet<>();

        for (UserRole userRole : user.getUserRoles()) {
            Role role = userRole.getRole();
            if (role != null && !role.isDeleted() && role.isActive()) {
                roleCodes.add(role.getCode());
                role.getRolePermissions().stream()
                        .map(rp -> rp.getPermission())
                        .filter(Objects::nonNull)
                        .forEach(perm -> permissionCodes.add(perm.getCode()));
            }
        }

        return new CareOsUserPrincipal(
                user.getId(),
                user.getPublicId(),
                user.getUsername(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getFirstName(),
                user.getLastName(),
                user.getUserScope(),
                user.getHospital() != null ? user.getHospital().getId() : null,
                user.getHospital() != null ? user.getHospital().getHospitalCode() : null,
                user.getHospital() != null ? user.getHospital().getName() : null,
                user.getStatus(),
                user.isDeleted(),
                user.isMustChangePassword(),
                user.getLockedUntil(),
                user.getLastLoginAt(),
                user.getPasswordChangedAt(),
                roleCodes,
                permissionCodes
        );
    }

    public Long getUserId() {
        return userId;
    }

    public String getPublicId() {
        return publicId;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFullName() {
        return fullName;
    }

    public UserScope getUserScope() {
        return userScope;
    }

    public Long getHospitalId() {
        return hospitalId;
    }

    public String getHospitalCode() {
        return hospitalCode;
    }

    public String getHospitalName() {
        return hospitalName;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
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

    public Set<String> getRoles() {
        return roles;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public boolean isPlatformUser() {
        return userScope == UserScope.PLATFORM && hospitalId == null;
    }

    public boolean isHospitalUser() {
        return userScope == UserScope.HOSPITAL && hospitalId != null;
    }

    public boolean hasPermission(String permissionCode) {
        return permissions.contains(permissionCode);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != AccountStatus.LOCKED
                && (lockedUntil == null || !lockedUntil.isAfter(Instant.now()));
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return !deleted && status == AccountStatus.ACTIVE;
    }
}
