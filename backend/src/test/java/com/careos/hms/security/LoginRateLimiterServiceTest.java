package com.careos.hms.security;

import com.careos.hms.common.exception.TooManyRequestsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginRateLimiterServiceTest {

    private LoginRateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        CareOsSecurityProperties properties = new CareOsSecurityProperties();
        properties.getLogin().setRateLimitMaxAttempts(3);
        properties.getLogin().setRateLimitWindowSeconds(60);
        rateLimiterService = new LoginRateLimiterService(properties);
    }

    @Test
    void shouldThrottleWhenAttemptThresholdExceededAndResetOnClear() {
        String ip = "192.0.2.10";
        String identifier = "doctor.strange";

        assertThatCode(() -> {
            rateLimiterService.checkAndRecordAttempt(ip, identifier);
            rateLimiterService.checkAndRecordAttempt(ip, identifier);
            rateLimiterService.checkAndRecordAttempt(ip, identifier);
        }).doesNotThrowAnyException();

        assertThatThrownBy(() -> rateLimiterService.checkAndRecordAttempt(ip, identifier))
                .isInstanceOf(TooManyRequestsException.class);

        rateLimiterService.clearAttempts(ip, identifier);

        assertThatCode(() -> rateLimiterService.checkAndRecordAttempt(ip, identifier))
                .doesNotThrowAnyException();
    }
}
