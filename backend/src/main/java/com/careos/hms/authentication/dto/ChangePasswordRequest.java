package com.careos.hms.authentication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Current password is required")
        @Size(max = 128, message = "Current password length is invalid")
        String currentPassword,

        @NotBlank(message = "New password is required")
        @Size(min = 12, max = 128, message = "New password must be between 12 and 128 characters")
        String newPassword,

        @NotBlank(message = "Password confirmation is required")
        @Size(min = 12, max = 128, message = "Confirmation password length is invalid")
        String confirmPassword
) {
}
