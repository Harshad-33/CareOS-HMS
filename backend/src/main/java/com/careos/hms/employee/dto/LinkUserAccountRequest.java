package com.careos.hms.employee.dto;

import jakarta.validation.constraints.Size;

import java.util.Set;

public record LinkUserAccountRequest(
        // Existing user public ID to link
        String userPublicId,

        // OR create and link a new user account:
        @Size(min = 3, max = 64, message = "Username must be between 3 and 64 characters")
        String username,

        String password,

        Set<String> roleCodes
) {
}
