package com.careos.hms.security;

import com.careos.hms.common.exception.ForbiddenOperationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Enforces hospital-level tenant boundaries and platform-vs-hospital isolation rules
 * using server-resolved authenticated principal claims.
 */
@Component("tenantGuard")
public class TenantAccessGuard {

    private static final Logger log = LoggerFactory.getLogger(TenantAccessGuard.class);

    private final TenantContext tenantContext;

    public TenantAccessGuard(TenantContext tenantContext) {
        this.tenantContext = tenantContext;
    }

    public Long requireHospitalId() {
        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        if (!principal.isHospitalUser() || principal.getHospitalId() == null) {
            throw new ForbiddenOperationException(
                    "TENANT_CONTEXT_REQUIRED",
                    "A hospital-scoped user context is required for this operation"
            );
        }
        return principal.getHospitalId();
    }

    public void assertSameHospital(Long targetHospitalId) {
        if (!canAccessHospital(targetHospitalId)) {
            CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
            log.warn(
                    "Cross-hospital access denied: userPublicId={}, userScope={}, userHospitalId={}, targetHospitalId={}",
                    principal.getPublicId(),
                    principal.getUserScope(),
                    principal.getHospitalId(),
                    targetHospitalId
            );
            throw new ForbiddenOperationException(
                    "CROSS_TENANT_ACCESS_DENIED",
                    "Access denied for requested hospital resource"
            );
        }
    }

    public boolean canAccessHospital(Long targetHospitalId) {
        if (targetHospitalId == null) {
            return false;
        }
        CareOsUserPrincipal principal = tenantContext.findCurrentPrincipal().orElse(null);
        if (principal == null || !principal.isHospitalUser()) {
            return false;
        }
        return Objects.equals(principal.getHospitalId(), targetHospitalId);
    }

    public boolean canAccessPlatform() {
        CareOsUserPrincipal principal = tenantContext.findCurrentPrincipal().orElse(null);
        return principal != null && principal.isPlatformUser();
    }

    public void assertPlatformScope() {
        if (!canAccessPlatform()) {
            throw new ForbiddenOperationException(
                    "PLATFORM_SCOPE_REQUIRED",
                    "Platform administrator scope is required for this operation"
            );
        }
    }
}
