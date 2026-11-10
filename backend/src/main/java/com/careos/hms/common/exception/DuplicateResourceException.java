package com.careos.hms.common.exception;

import org.springframework.http.HttpStatus;

public class DuplicateResourceException extends CareOsException {

    public DuplicateResourceException(String message) {
        super(HttpStatus.CONFLICT, "DUPLICATE_RESOURCE", message);
    }
}
