package com.careos.hms.authentication.dto;

import com.careos.hms.user.entity.AccountStatus;
import com.careos.hms.user.entity.UserScope;

import java.time.Instant;
import java.util.Set;

public record AuthUserDto(
        String publicId,
        String username,
        String email,
        String firstName,
        String lastName,
        String fullName,
        UserScope userScope,
        Long hospitalId,
        String hospitalCode,
        String hospitalName,
        Set<String> roles,
        Set<String> permissions,
        AccountStatus status,
        boolean mustChangePassword,
        Instant lastLoginAt
) {
}
