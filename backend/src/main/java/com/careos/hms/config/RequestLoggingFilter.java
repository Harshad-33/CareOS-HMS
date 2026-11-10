package com.careos.hms.config;

import com.careos.hms.common.constants.ApiConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final Pattern SAFE_CORRELATION_ID_PATTERN = Pattern.compile("^[A-Za-z0-9\\-_]{1,64}$");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String correlationId = resolveCorrelationId(request);
        long startNanos = System.nanoTime();

        MDC.put(ApiConstants.CORRELATION_ID_MDC_KEY, correlationId);
        response.setHeader(ApiConstants.CORRELATION_ID_HEADER, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
            String uri = request.getRequestURI();
            if (uri.startsWith(ApiConstants.API_V1_BASE_PATH)) {
                log.info("HTTP {} {} -> {} ({} ms)", request.getMethod(), uri, response.getStatus(), durationMs);
            } else {
                log.debug("HTTP {} {} -> {} ({} ms)", request.getMethod(), uri, response.getStatus(), durationMs);
            }
            MDC.remove(ApiConstants.CORRELATION_ID_MDC_KEY);
        }
    }

    private String resolveCorrelationId(HttpServletRequest request) {
        String incoming = request.getHeader(ApiConstants.CORRELATION_ID_HEADER);
        if (incoming != null && SAFE_CORRELATION_ID_PATTERN.matcher(incoming.trim()).matches()) {
            return incoming.trim();
        }
        return UUID.randomUUID().toString();
    }
}
