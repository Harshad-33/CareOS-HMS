package com.careos.hms.employee.entity;

public enum EmployeeStatus {
    ACTIVE,
    INACTIVE,
    SUSPENDED,
    RESIGNED,
    TERMINATED;

    public boolean canTransitionTo(EmployeeStatus target) {
        if (target == null || this == target) {
            return false;
        }
        return switch (this) {
            case ACTIVE -> target == SUSPENDED || target == INACTIVE || target == RESIGNED || target == TERMINATED;
            case SUSPENDED -> target == ACTIVE || target == RESIGNED || target == TERMINATED;
            case INACTIVE -> target == ACTIVE || target == RESIGNED || target == TERMINATED;
            case RESIGNED -> target == INACTIVE;
            case TERMINATED -> false; // Terminal state
        };
    }
}
