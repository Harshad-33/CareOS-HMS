package com.careos.hms.authentication.dto;

public record AuthTokenResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        String csrfToken,
        AuthUserDto user
) {
}
