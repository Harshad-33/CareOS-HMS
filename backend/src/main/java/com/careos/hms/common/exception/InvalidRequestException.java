package com.careos.hms.common.exception;

import org.springframework.http.HttpStatus;

public class InvalidRequestException extends CareOsException {

    public InvalidRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", message);
    }
}
