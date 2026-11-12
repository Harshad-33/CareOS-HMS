package com.careos.hms.common.exception;

import org.springframework.http.HttpStatus;

public class AccountLockedException extends CareOsException {

    public AccountLockedException(String message) {
        super(HttpStatus.LOCKED, "ACCOUNT_LOCKED", message);
    }
}
