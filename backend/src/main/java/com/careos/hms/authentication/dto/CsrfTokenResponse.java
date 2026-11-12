package com.careos.hms.authentication.dto;

public record CsrfTokenResponse(
        String csrfToken,
        String headerName
) {
}
