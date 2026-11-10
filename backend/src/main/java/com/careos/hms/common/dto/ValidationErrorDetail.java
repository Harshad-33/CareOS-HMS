package com.careos.hms.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ValidationErrorDetail(
        String field,
        String message
) {
}
