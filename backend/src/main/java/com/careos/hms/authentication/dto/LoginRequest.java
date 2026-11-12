package com.careos.hms.authentication.dto;

import com.careos.hms.common.validation.TrimmedNotBlank;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @TrimmedNotBlank(message = "Username or email is required")
        @Size(max = 160, message = "Username or email must not exceed 160 characters")
        String usernameOrEmail,

        @NotBlank(message = "Password is required")
        @Size(max = 128, message = "Password length is invalid")
        String password,

        Boolean rememberMe
) {
    public boolean isRememberMeEnabled() {
        return Boolean.TRUE.equals(rememberMe);
    }
}
