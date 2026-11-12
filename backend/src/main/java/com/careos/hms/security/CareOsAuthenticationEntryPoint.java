package com.careos.hms.security;

import com.careos.hms.common.constants.ApiConstants;
import com.careos.hms.common.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

@Component
public class CareOsAuthenticationEntryPoint implements AuthenticationEntryPoint {

    public static final String AUTH_ERROR_CODE_ATTR = "careos.auth.error.code";
    public static final String AUTH_ERROR_MESSAGE_ATTR = "careos.auth.error.message";

    private final ObjectMapper objectMapper;

    public CareOsAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        Object customCode = request.getAttribute(AUTH_ERROR_CODE_ATTR);
        Object customMessage = request.getAttribute(AUTH_ERROR_MESSAGE_ATTR);

        String code = customCode instanceof String s && !s.isBlank() ? s : "UNAUTHORIZED";
        String message = customMessage instanceof String s && !s.isBlank()
                ? s
                : "Authentication is required to access this resource";

        ErrorResponse body = new ErrorResponse(
                false,
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                code,
                message,
                request.getRequestURI(),
                MDC.get(ApiConstants.CORRELATION_ID_MDC_KEY),
                Instant.now(),
                null
        );

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }
}
