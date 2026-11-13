package com.careos.hms.hospital.dto;

import com.careos.hms.hospital.entity.ContactType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record HospitalContactRequest(
        @NotNull(message = "Contact type is required")
        ContactType contactType,

        @NotBlank(message = "Contact name is required")
        @Size(max = 100)
        String name,

        @Size(max = 100)
        String designation,

        @NotBlank(message = "Phone number is required")
        @Size(max = 32)
        String phoneNumber,

        @Email(message = "Invalid email format")
        @Size(max = 160)
        String email,

        boolean primary
) {}
