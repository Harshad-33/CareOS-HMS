package com.careos.hms.security;

import com.careos.hms.common.constants.ApiConstants;
import com.careos.hms.common.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

@Component
public class CareOsAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public CareOsAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        writeForbiddenResponse(response, request, "ACCESS_DENIED", "You do not have permission to perform this action");
    }

    public void writeForbiddenResponse(
            HttpServletResponse response,
            HttpServletRequest request,
            String code,
            String message
    ) throws IOException {
        ErrorResponse body = new ErrorResponse(
                false,
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                code,
                message,
                request.getRequestURI(),
                MDC.get(ApiConstants.CORRELATION_ID_MDC_KEY),
                Instant.now(),
                null
        );

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }
}
