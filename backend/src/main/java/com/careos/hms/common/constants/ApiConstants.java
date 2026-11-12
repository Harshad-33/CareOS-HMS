package com.careos.hms.common.constants;

public final class ApiConstants {

    private ApiConstants() {
    }

    public static final String API_V1_BASE_PATH = "/api/v1";
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";
    public static final String CSRF_TOKEN_HEADER = "X-CSRF-Token";

    public static final int DEFAULT_PAGE_NUMBER = 0;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    public static final String DEFAULT_SORT_FIELD = "id";
    public static final String DEFAULT_SORT_DIRECTION = "ASC";

    public static final String SYSTEM_AUDITOR = "SYSTEM";
}
