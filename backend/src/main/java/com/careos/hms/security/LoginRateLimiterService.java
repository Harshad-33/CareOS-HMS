package com.careos.hms.security;

import com.careos.hms.common.exception.TooManyRequestsException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginRateLimiterService {

    private final CareOsSecurityProperties securityProperties;
    private final ConcurrentHashMap<String, Deque<Instant>> attemptsByKey = new ConcurrentHashMap<>();

    public LoginRateLimiterService(CareOsSecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    public void checkAndRecordAttempt(String clientIp, String identifier) {
        int maxAttempts = securityProperties.getLogin().getRateLimitMaxAttempts();
        long windowSeconds = securityProperties.getLogin().getRateLimitWindowSeconds();
        if (maxAttempts <= 0 || windowSeconds <= 0) {
            return;
        }

        Instant now = Instant.now();
        Instant cutoff = now.minusSeconds(windowSeconds);
        String key = buildKey(clientIp, identifier);

        Deque<Instant> timestamps = attemptsByKey.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(cutoff)) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= maxAttempts) {
                throw new TooManyRequestsException(
                        "Too many login attempts from this source. Please wait before trying again."
                );
            }
            timestamps.addLast(now);
        }
    }

    public void clearAttempts(String clientIp, String identifier) {
        attemptsByKey.remove(buildKey(clientIp, identifier));
    }

    public void resetAll() {
        attemptsByKey.clear();
    }

    private String buildKey(String clientIp, String identifier) {
        String safeIp = clientIp != null ? clientIp.trim() : "unknown";
        String safeId = identifier != null ? identifier.trim().toLowerCase(Locale.ROOT) : "";
        return safeIp + "::" + safeId;
    }
}
