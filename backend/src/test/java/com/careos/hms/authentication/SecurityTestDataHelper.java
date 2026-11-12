package com.careos.hms.authentication;

import com.careos.hms.authentication.repository.RefreshTokenRepository;
import com.careos.hms.hospital.entity.Hospital;
import com.careos.hms.hospital.repository.HospitalRepository;
import com.careos.hms.permission.entity.Permission;
import com.careos.hms.permission.entity.PermissionCode;
import com.careos.hms.permission.entity.PermissionScope;
import com.careos.hms.permission.entity.RolePermission;
import com.careos.hms.permission.repository.PermissionRepository;
import com.careos.hms.permission.repository.RolePermissionRepository;
import com.careos.hms.role.entity.Role;
import com.careos.hms.role.entity.RoleScope;
import com.careos.hms.role.entity.SystemRoleCode;
import com.careos.hms.role.entity.UserRole;
import com.careos.hms.role.repository.RoleRepository;
import com.careos.hms.role.repository.UserRoleRepository;
import com.careos.hms.user.entity.User;
import com.careos.hms.user.entity.UserScope;
import com.careos.hms.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class SecurityTestDataHelper {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final HospitalRepository hospitalRepository;
    private final PasswordEncoder passwordEncoder;

    public SecurityTestDataHelper(
            RefreshTokenRepository refreshTokenRepository,
            UserRoleRepository userRoleRepository,
            RolePermissionRepository rolePermissionRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            HospitalRepository hospitalRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRoleRepository = userRoleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.hospitalRepository = hospitalRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void resetSecurityTables() {
        refreshTokenRepository.deleteAllInBatch();
        userRoleRepository.deleteAllInBatch();
        rolePermissionRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        roleRepository.deleteAllInBatch();
        permissionRepository.deleteAllInBatch();
        hospitalRepository.deleteAllInBatch();
    }

    @Transactional
    public SeededRbac seedRbacCatalog() {
        Role superAdminRole = roleRepository.saveAndFlush(new Role(
                SystemRoleCode.PLATFORM_SUPER_ADMIN,
                "Platform Super Administrator",
                "Global platform owner role",
                RoleScope.PLATFORM,
                true
        ));
        Role hospitalAdminRole = roleRepository.saveAndFlush(new Role(
                SystemRoleCode.HOSPITAL_ADMIN,
                "Hospital Administrator",
                "Hospital tenant administrator role",
                RoleScope.HOSPITAL,
                true
        ));

        Map<String, Permission> permissions = new LinkedHashMap<>();
        permissions.put(PermissionCode.AUTH_ME_READ, createPerm(PermissionCode.AUTH_ME_READ, "Read Current User", "authentication", PermissionScope.COMMON));
        permissions.put(PermissionCode.AUTH_PERMISSIONS_READ, createPerm(PermissionCode.AUTH_PERMISSIONS_READ, "Read User Permissions", "authentication", PermissionScope.COMMON));
        permissions.put(PermissionCode.AUTH_PASSWORD_CHANGE, createPerm(PermissionCode.AUTH_PASSWORD_CHANGE, "Change Own Password", "authentication", PermissionScope.COMMON));
        permissions.put(PermissionCode.SYSTEM_HEALTH_VIEW, createPerm(PermissionCode.SYSTEM_HEALTH_VIEW, "View System Health", "health", PermissionScope.COMMON));
        permissions.put(PermissionCode.PLATFORM_OVERVIEW_READ, createPerm(PermissionCode.PLATFORM_OVERVIEW_READ, "Read Platform Overview", "platform", PermissionScope.PLATFORM));
        permissions.put(PermissionCode.PLATFORM_HOSPITALS_MANAGE, createPerm(PermissionCode.PLATFORM_HOSPITALS_MANAGE, "Manage Hospitals", "platform", PermissionScope.PLATFORM));
        permissions.put(PermissionCode.PLATFORM_AUDIT_READ, createPerm(PermissionCode.PLATFORM_AUDIT_READ, "Read Platform Audit", "platform", PermissionScope.PLATFORM));
        permissions.put(PermissionCode.HOSPITAL_WORKSPACE_READ, createPerm(PermissionCode.HOSPITAL_WORKSPACE_READ, "Read Hospital Workspace", "hospital", PermissionScope.HOSPITAL));
        permissions.put(PermissionCode.HOSPITAL_USERS_MANAGE, createPerm(PermissionCode.HOSPITAL_USERS_MANAGE, "Manage Hospital Users", "users", PermissionScope.HOSPITAL));
        permissions.put(PermissionCode.HOSPITAL_ROLES_READ, createPerm(PermissionCode.HOSPITAL_ROLES_READ, "Read Hospital Roles", "roles", PermissionScope.HOSPITAL));
        permissions.put(PermissionCode.HOSPITAL_SETTINGS_MANAGE, createPerm(PermissionCode.HOSPITAL_SETTINGS_MANAGE, "Manage Hospital Settings", "hospital", PermissionScope.HOSPITAL));

        for (String code : List.of(
                PermissionCode.AUTH_ME_READ,
                PermissionCode.AUTH_PERMISSIONS_READ,
                PermissionCode.AUTH_PASSWORD_CHANGE,
                PermissionCode.SYSTEM_HEALTH_VIEW,
                PermissionCode.PLATFORM_OVERVIEW_READ,
                PermissionCode.PLATFORM_HOSPITALS_MANAGE,
                PermissionCode.PLATFORM_AUDIT_READ
        )) {
            rolePermissionRepository.saveAndFlush(new RolePermission(superAdminRole, permissions.get(code)));
        }

        for (String code : List.of(
                PermissionCode.AUTH_ME_READ,
                PermissionCode.AUTH_PERMISSIONS_READ,
                PermissionCode.AUTH_PASSWORD_CHANGE,
                PermissionCode.SYSTEM_HEALTH_VIEW,
                PermissionCode.HOSPITAL_WORKSPACE_READ,
                PermissionCode.HOSPITAL_USERS_MANAGE,
                PermissionCode.HOSPITAL_ROLES_READ,
                PermissionCode.HOSPITAL_SETTINGS_MANAGE
        )) {
            rolePermissionRepository.saveAndFlush(new RolePermission(hospitalAdminRole, permissions.get(code)));
        }

        return new SeededRbac(superAdminRole, hospitalAdminRole, permissions);
    }

    @Transactional
    public Hospital createHospital(String code, String name) {
        return hospitalRepository.saveAndFlush(new Hospital(code, name));
    }

    @Transactional
    public User createUser(
            Hospital hospital,
            UserScope scope,
            String username,
            String email,
            String rawPassword,
            Role role
    ) {
        User user = userRepository.saveAndFlush(new User(
                hospital,
                scope,
                username,
                email,
                passwordEncoder.encode(rawPassword),
                "Test",
                "User"
        ));
        if (role != null) {
            userRoleRepository.saveAndFlush(new UserRole(user, role, "TEST_SEED"));
        }
        return user;
    }

    private Permission createPerm(String code, String name, String moduleName, PermissionScope scope) {
        return permissionRepository.saveAndFlush(new Permission(code, name, moduleName, scope, name));
    }

    public record SeededRbac(
            Role platformSuperAdminRole,
            Role hospitalAdminRole,
            Map<String, Permission> permissions
    ) {
    }
}
