package com.careos.hms.common.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends CareOsException {

    public UnauthorizedException(String errorCode, String message) {
        super(HttpStatus.UNAUTHORIZED, errorCode, message);
    }

    public UnauthorizedException(String message) {
        this("UNAUTHORIZED", message);
    }
}
