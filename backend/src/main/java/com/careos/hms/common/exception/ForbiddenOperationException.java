package com.careos.hms.common.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenOperationException extends CareOsException {

    public ForbiddenOperationException(String errorCode, String message) {
        super(HttpStatus.FORBIDDEN, errorCode, message);
    }

    public ForbiddenOperationException(String message) {
        this("ACCESS_DENIED", message);
    }
}
