package com.careos.hms.hospital.entity;

public enum HospitalStatus {
    PENDING,
    ACTIVE,
    INACTIVE,
    SUSPENDED;

    public boolean canTransitionTo(HospitalStatus target) {
        if (target == null || this == target) {
            return false;
        }
        return switch (this) {
            case PENDING -> target == ACTIVE || target == INACTIVE;
            case ACTIVE -> target == SUSPENDED || target == INACTIVE;
            case SUSPENDED -> target == ACTIVE || target == INACTIVE;
            case INACTIVE -> target == ACTIVE;
        };
    }
}

