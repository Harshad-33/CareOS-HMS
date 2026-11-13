-- CareOS-HMS Phase 2 Initial System Roles & Permissions Seed
-- Note: Zero user credentials are seeded in migrations. PLATFORM_SUPER_ADMIN is bootstrapped securely.

INSERT INTO roles (code, name, description, scope, is_system_role, is_active, created_by, updated_by)
VALUES
    ('PLATFORM_SUPER_ADMIN', 'Platform Super Administrator', 'Global platform-level administrator with cross-system governance permissions.', 'PLATFORM', 1, 1, 'SYSTEM', 'SYSTEM'),
    ('HOSPITAL_ADMIN', 'Hospital Administrator', 'Tenant-scoped administrator restricted strictly to their assigned hospital.', 'HOSPITAL', 1, 1, 'SYSTEM', 'SYSTEM');

INSERT INTO permissions (code, name, module_name, scope, description, created_by, updated_by)
VALUES
    ('AUTH_ME_READ', 'Read Own Profile', 'authentication', 'COMMON', 'Retrieve authenticated user profile and security context.', 'SYSTEM', 'SYSTEM'),
    ('AUTH_PERMISSIONS_READ', 'Read Own Permissions', 'authentication', 'COMMON', 'Retrieve assigned roles and resolved permissions for the current user.', 'SYSTEM', 'SYSTEM'),
    ('AUTH_PASSWORD_CHANGE', 'Change Own Password', 'authentication', 'COMMON', 'Rotate password for the authenticated user account.', 'SYSTEM', 'SYSTEM'),
    ('SYSTEM_HEALTH_VIEW', 'View System Workspace Health', 'dashboard', 'COMMON', 'Access the operational system overview and service health card.', 'SYSTEM', 'SYSTEM'),
    ('PLATFORM_OVERVIEW_READ', 'View Platform Overview', 'platform', 'PLATFORM', 'Access platform-level administrative overview.', 'SYSTEM', 'SYSTEM'),
    ('PLATFORM_HOSPITALS_MANAGE', 'Manage Platform Hospitals', 'hospital', 'PLATFORM', 'Platform-level capability for future hospital onboarding and governance.', 'SYSTEM', 'SYSTEM'),
    ('PLATFORM_AUDIT_READ', 'Read Platform Security Audit', 'audit', 'PLATFORM', 'Read platform-level security and governance telemetry.', 'SYSTEM', 'SYSTEM'),
    ('HOSPITAL_WORKSPACE_READ', 'View Hospital Workspace', 'hospital', 'HOSPITAL', 'View workspace metrics and configuration scoped to own hospital.', 'SYSTEM', 'SYSTEM'),
    ('HOSPITAL_USERS_MANAGE', 'Manage Hospital Users', 'user', 'HOSPITAL', 'Manage user accounts belonging strictly to own hospital.', 'SYSTEM', 'SYSTEM'),
    ('HOSPITAL_ROLES_READ', 'View Hospital Roles', 'role', 'HOSPITAL', 'View role assignments and available hospital permissions.', 'SYSTEM', 'SYSTEM'),
    ('HOSPITAL_SETTINGS_MANAGE', 'Manage Hospital Settings', 'hospital', 'HOSPITAL', 'Manage settings strictly within own hospital tenant boundary.', 'SYSTEM', 'SYSTEM');

-- PLATFORM_SUPER_ADMIN permissions (explicit platform + common self-service permissions)
INSERT INTO role_permissions (role_id, permission_id, created_by, updated_by)
SELECT r.id, p.id, 'SYSTEM', 'SYSTEM'
FROM roles r
JOIN permissions p ON p.code IN (
    'AUTH_ME_READ',
    'AUTH_PERMISSIONS_READ',
    'AUTH_PASSWORD_CHANGE',
    'SYSTEM_HEALTH_VIEW',
    'PLATFORM_OVERVIEW_READ',
    'PLATFORM_HOSPITALS_MANAGE',
    'PLATFORM_AUDIT_READ'
)
WHERE r.code = 'PLATFORM_SUPER_ADMIN';

-- HOSPITAL_ADMIN permissions (strictly hospital-scoped + common self-service permissions)
INSERT INTO role_permissions (role_id, permission_id, created_by, updated_by)
SELECT r.id, p.id, 'SYSTEM', 'SYSTEM'
FROM roles r
JOIN permissions p ON p.code IN (
    'AUTH_ME_READ',
    'AUTH_PERMISSIONS_READ',
    'AUTH_PASSWORD_CHANGE',
    'SYSTEM_HEALTH_VIEW',
    'HOSPITAL_WORKSPACE_READ',
    'HOSPITAL_USERS_MANAGE',
    'HOSPITAL_ROLES_READ',
    'HOSPITAL_SETTINGS_MANAGE'
)
WHERE r.code = 'HOSPITAL_ADMIN';
