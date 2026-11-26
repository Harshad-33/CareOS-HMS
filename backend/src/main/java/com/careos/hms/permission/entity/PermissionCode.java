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

    // Phase 3 Hospital Administration Permissions
    public static final String HOSPITAL_DEPARTMENTS_MANAGE = "HOSPITAL_DEPARTMENTS_MANAGE";
    public static final String HOSPITAL_BRANCHES_MANAGE = "HOSPITAL_BRANCHES_MANAGE";
    public static final String HOSPITAL_DOCUMENTS_MANAGE = "HOSPITAL_DOCUMENTS_MANAGE";
    public static final String HOSPITAL_CONTACTS_MANAGE = "HOSPITAL_CONTACTS_MANAGE";
    public static final String HOSPITAL_PROFILE_UPDATE = "HOSPITAL_PROFILE_UPDATE";
    public static final String HOSPITAL_AUDIT_READ = "HOSPITAL_AUDIT_READ";
    public static final String PLATFORM_DOCUMENTS_VERIFY = "PLATFORM_DOCUMENTS_VERIFY";

    // Phase 4 Employee Management Permissions
    public static final String HOSPITAL_EMPLOYEES_READ = "HOSPITAL_EMPLOYEES_READ";
    public static final String HOSPITAL_EMPLOYEES_MANAGE = "HOSPITAL_EMPLOYEES_MANAGE";
    public static final String HOSPITAL_EMPLOYEES_STATUS = "HOSPITAL_EMPLOYEES_STATUS";
    public static final String HOSPITAL_EMPLOYEES_ACCOUNT_LINK = "HOSPITAL_EMPLOYEES_ACCOUNT_LINK";
    public static final String PLATFORM_EMPLOYEES_READ = "PLATFORM_EMPLOYEES_READ";

    // Phase 5 HR Management Permissions
    public static final String PLATFORM_HR_READ = "PLATFORM_HR_READ";
    public static final String HOSPITAL_HR_SELF_SERVICE = "HOSPITAL_HR_SELF_SERVICE";
    public static final String HOSPITAL_LEAVES_APPLY = "HOSPITAL_LEAVES_APPLY";
    public static final String HOSPITAL_LEAVES_READ = "HOSPITAL_LEAVES_READ";
    public static final String HOSPITAL_LEAVES_APPROVE = "HOSPITAL_LEAVES_APPROVE";
    public static final String HOSPITAL_LEAVES_MANAGE = "HOSPITAL_LEAVES_MANAGE";
    public static final String HOSPITAL_ATTENDANCE_RECORD = "HOSPITAL_ATTENDANCE_RECORD";
    public static final String HOSPITAL_ATTENDANCE_READ = "HOSPITAL_ATTENDANCE_READ";
    public static final String HOSPITAL_ATTENDANCE_APPROVE = "HOSPITAL_ATTENDANCE_APPROVE";
    public static final String HOSPITAL_ATTENDANCE_MANAGE = "HOSPITAL_ATTENDANCE_MANAGE";
    public static final String HOSPITAL_SHIFTS_READ = "HOSPITAL_SHIFTS_READ";
    public static final String HOSPITAL_SHIFTS_MANAGE = "HOSPITAL_SHIFTS_MANAGE";
    public static final String HOSPITAL_SHIFTS_SWAP = "HOSPITAL_SHIFTS_SWAP";
    public static final String HOSPITAL_HOLIDAYS_READ = "HOSPITAL_HOLIDAYS_READ";
    public static final String HOSPITAL_HOLIDAYS_MANAGE = "HOSPITAL_HOLIDAYS_MANAGE";
    public static final String HOSPITAL_HR_DOCUMENTS_READ = "HOSPITAL_HR_DOCUMENTS_READ";
    public static final String HOSPITAL_HR_DOCUMENTS_MANAGE = "HOSPITAL_HR_DOCUMENTS_MANAGE";
    public static final String HOSPITAL_HR_REPORTS_READ = "HOSPITAL_HR_REPORTS_READ";
}

