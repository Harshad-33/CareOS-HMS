package com.careos.hms.common.exception;

import org.springframework.http.HttpStatus;

public class TooManyRequestsException extends CareOsException {

    public TooManyRequestsException(String message) {
        super(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED", message);
    }
}
