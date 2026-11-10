package com.careos.hms.common.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends CareOsException {

    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", message);
    }

    public ResourceNotFoundException(String resourceName, Object identifier) {
        super(
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                String.format("%s not found with identifier: %s", resourceName, identifier)
        );
    }
}
