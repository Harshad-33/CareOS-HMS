package com.careos.hms.common.response;

import com.careos.hms.common.constants.ApiConstants;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.MDC;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        Instant timestamp,
        String path,
        String correlationId
) {

    public static <T> ApiResponse<T> ok(T data, String message, String path) {
        return new ApiResponse<>(
                true,
                message,
                data,
                Instant.now(),
                path,
                MDC.get(ApiConstants.CORRELATION_ID_MDC_KEY)
        );
    }

    public static <T> ApiResponse<T> ok(T data, String path) {
        return ok(data, "Request processed successfully", path);
    }

    public static <T> ApiResponse<T> created(T data, String message, String path) {
        return ok(data, message, path);
    }

    public static ApiResponse<Void> message(String message, String path) {
        return ok(null, message, path);
    }
}

