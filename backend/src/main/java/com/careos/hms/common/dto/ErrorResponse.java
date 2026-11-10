package com.careos.hms.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        boolean success,
        int status,
        String error,
        String code,
        String message,
        String path,
        String correlationId,
        Instant timestamp,
        List<ValidationErrorDetail> fieldErrors
) {
}
