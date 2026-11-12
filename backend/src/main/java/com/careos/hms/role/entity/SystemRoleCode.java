package com.careos.hms.role.entity;

/**
 * Canonical role code constants.
 * Phase 2 seeds only PLATFORM_SUPER_ADMIN and HOSPITAL_ADMIN;
 * clinical and operational roles are reserved for upcoming modules.
 */
public final class SystemRoleCode {

    private SystemRoleCode() {
    }

    // Seeded in Phase 2
    public static final String PLATFORM_SUPER_ADMIN = "PLATFORM_SUPER_ADMIN";
    public static final String HOSPITAL_ADMIN = "HOSPITAL_ADMIN";

    // Designed for future CareOS-HMS phases
    public static final String DOCTOR = "DOCTOR";
    public static final String NURSE = "NURSE";
    public static final String RECEPTIONIST = "RECEPTIONIST";
    public static final String PHARMACIST = "PHARMACIST";
    public static final String LAB_TECHNICIAN = "LAB_TECHNICIAN";
    public static final String ACCOUNTANT = "ACCOUNTANT";
    public static final String HOSPITAL_STAFF = "HOSPITAL_STAFF";
}
