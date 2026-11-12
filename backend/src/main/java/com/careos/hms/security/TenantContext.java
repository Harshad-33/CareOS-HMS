package com.careos.hms.security;

import com.careos.hms.common.exception.UnauthorizedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Server-side authenticated security and tenant context resolver.
 * Never resolves tenant identifiers from untrusted HTTP request payloads or headers.
 */
@Component
public class TenantContext {

    public Optional<CareOsUserPrincipal> findCurrentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        if (authentication.getPrincipal() instanceof CareOsUserPrincipal principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }

    public CareOsUserPrincipal getRequiredPrincipal() {
        return findCurrentPrincipal()
                .orElseThrow(() -> new UnauthorizedException("AUTHENTICATION_REQUIRED", "Authentication is required"));
    }

    public Long getCurrentUserId() {
        return getRequiredPrincipal().getUserId();
    }

    public Long getCurrentHospitalId() {
        return getRequiredPrincipal().getHospitalId();
    }

    public boolean isPlatformUser() {
        return getRequiredPrincipal().isPlatformUser();
    }

    public boolean isHospitalUser() {
        return getRequiredPrincipal().isHospitalUser();
    }
}
