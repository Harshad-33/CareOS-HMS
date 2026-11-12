package com.careos.hms.authentication.dto;

import com.careos.hms.user.entity.UserScope;

import java.util.Set;

public record UserPermissionsResponse(
        UserScope userScope,
        Long hospitalId,
        Set<String> roles,
        Set<String> permissions
) {
}
