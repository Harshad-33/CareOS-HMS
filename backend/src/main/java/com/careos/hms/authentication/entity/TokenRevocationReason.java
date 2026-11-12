package com.careos.hms.authentication.entity;

public enum TokenRevocationReason {
    ROTATED,
    LOGOUT,
    LOGOUT_ALL,
    REUSE_DETECTED,
    PASSWORD_CHANGED,
    ACCOUNT_DISABLED,
    EXPIRED
}
