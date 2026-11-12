package com.careos.hms.permission.entity;

/**
 * Canonical Phase 2 permission code constants matching seeded rows in permissions table.
 * Additional domain permissions will be appended in future modules.
 */
public final class PermissionCode {

    private PermissionCode() {
    }

    public static final String AUTH_ME_READ = "AUTH_ME_READ";
    public static final String AUTH_PERMISSIONS_READ = "AUTH_PERMISSIONS_READ";
    public static final String AUTH_PASSWORD_CHANGE = "AUTH_PASSWORD_CHANGE";
    public static final String SYSTEM_HEALTH_VIEW = "SYSTEM_HEALTH_VIEW";

    public static final String PLATFORM_OVERVIEW_READ = "PLATFORM_OVERVIEW_READ";
    public static final String PLATFORM_HOSPITALS_MANAGE = "PLATFORM_HOSPITALS_MANAGE";
    public static final String PLATFORM_AUDIT_READ = "PLATFORM_AUDIT_READ";

    public static final String HOSPITAL_WORKSPACE_READ = "HOSPITAL_WORKSPACE_READ";
    public static final String HOSPITAL_USERS_MANAGE = "HOSPITAL_USERS_MANAGE";
    public static final String HOSPITAL_ROLES_READ = "HOSPITAL_ROLES_READ";
    public static final String HOSPITAL_SETTINGS_MANAGE = "HOSPITAL_SETTINGS_MANAGE";
}
