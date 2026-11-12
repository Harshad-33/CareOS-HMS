package com.careos.hms.authentication.service;

import com.careos.hms.authentication.dto.AuthTokenResponse;
import com.careos.hms.authentication.dto.AuthUserDto;
import com.careos.hms.authentication.dto.ChangePasswordRequest;
import com.careos.hms.authentication.dto.CsrfTokenResponse;
import com.careos.hms.authentication.dto.LoginRequest;
import com.careos.hms.authentication.dto.UserPermissionsResponse;
import com.careos.hms.authentication.entity.TokenRevocationReason;
import com.careos.hms.authentication.mapper.AuthMapper;
import com.careos.hms.authentication.validator.PasswordPolicyValidator;
import com.careos.hms.common.constants.ApiConstants;
import com.careos.hms.common.exception.AccountLockedException;
import com.careos.hms.common.exception.CareOsException;
import com.careos.hms.common.exception.InvalidRequestException;
import com.careos.hms.common.exception.UnauthorizedException;
import com.careos.hms.hospital.entity.HospitalStatus;
import com.careos.hms.security.CareOsSecurityProperties;
import com.careos.hms.security.CareOsUserDetailsService;
import com.careos.hms.security.CareOsUserPrincipal;
import com.careos.hms.security.JwtTokenService;
import com.careos.hms.security.LoginRateLimiterService;
import com.careos.hms.security.TenantContext;
import com.careos.hms.user.entity.AccountStatus;
import com.careos.hms.user.entity.User;
import com.careos.hms.user.entity.UserScope;
import com.careos.hms.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);
    private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid username/email or password";

    private final UserRepository userRepository;
    private final CareOsUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;
    private final CookieService cookieService;
    private final LoginRateLimiterService rateLimiterService;
    private final PasswordPolicyValidator passwordPolicyValidator;
    private final TenantContext tenantContext;
    private final AuthMapper authMapper;
    private final CareOsSecurityProperties securityProperties;
    private final String dummyPasswordHash;

    public AuthenticationService(
            UserRepository userRepository,
            CareOsUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            RefreshTokenService refreshTokenService,
            CookieService cookieService,
            LoginRateLimiterService rateLimiterService,
            PasswordPolicyValidator passwordPolicyValidator,
            TenantContext tenantContext,
            AuthMapper authMapper,
            CareOsSecurityProperties securityProperties
    ) {
        this.userRepository = userRepository;
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenService = refreshTokenService;
        this.cookieService = cookieService;
        this.rateLimiterService = rateLimiterService;
        this.passwordPolicyValidator = passwordPolicyValidator;
        this.tenantContext = tenantContext;
        this.authMapper = authMapper;
        this.securityProperties = securityProperties;
        this.dummyPasswordHash = passwordEncoder.encode("careos-constant-time-guard");
    }

    public CsrfTokenResponse issueCsrfToken(HttpServletRequest request, HttpServletResponse response) {
        String token = cookieService.extractCsrfCookie(request)
                .orElseGet(() -> cookieService.generateAndWriteCsrfCookie(response));
        response.setHeader(ApiConstants.CSRF_TOKEN_HEADER, token);
        return new CsrfTokenResponse(token, ApiConstants.CSRF_TOKEN_HEADER);
    }

    @Transactional(noRollbackFor = CareOsException.class)
    public AuthTokenResponse login(
            LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String identifier = request.usernameOrEmail().trim();
        String clientIp = extractClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        rateLimiterService.checkAndRecordAttempt(clientIp, identifier);

        Optional<User> userOpt = userRepository.findActiveGraphByUsernameOrEmail(identifier);
        if (userOpt.isEmpty()) {
            passwordEncoder.matches(request.password(), dummyPasswordHash);
            log.warn("Failed login attempt for unknown identifier from ip={}", clientIp);
            throw new UnauthorizedException("INVALID_CREDENTIALS", INVALID_CREDENTIALS_MESSAGE);
        }

        User user = userOpt.get();
        Instant now = Instant.now();

        if (user.isTemporarilyLocked(now)) {
            log.warn("Locked account login blocked: userPublicId={}, ip={}", user.getPublicId(), clientIp);
            throw new AccountLockedException(
                    "Account is temporarily locked due to multiple failed login attempts. Please try again later."
            );
        }

        if (user.getStatus() != AccountStatus.ACTIVE || !isHospitalReadyForLogin(user)) {
            passwordEncoder.matches(request.password(), dummyPasswordHash);
            log.warn("Inactive account or inactive tenant login blocked: userPublicId={}, status={}",
                    user.getPublicId(), user.getStatus());
            throw new UnauthorizedException("INVALID_CREDENTIALS", INVALID_CREDENTIALS_MESSAGE);
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            user.recordFailedLogin(
                    now,
                    securityProperties.getLogin().getMaxFailedAttempts(),
                    securityProperties.getLogin().getLockoutDurationMinutes()
            );
            userRepository.saveAndFlush(user);

            if (user.isTemporarilyLocked(now)) {
                log.warn("Account temporarily locked after failed logins: userPublicId={}, attempts={}",
                        user.getPublicId(), user.getFailedLoginAttempts());
                throw new AccountLockedException(
                        "Account is temporarily locked due to multiple failed login attempts. Please try again later."
                );
            }

            log.warn("Invalid password login attempt: userPublicId={}, failedAttempts={}",
                    user.getPublicId(), user.getFailedLoginAttempts());
            throw new UnauthorizedException("INVALID_CREDENTIALS", INVALID_CREDENTIALS_MESSAGE);
        }

        user.recordSuccessfulLogin(now);
        userRepository.saveAndFlush(user);
        rateLimiterService.clearAttempts(clientIp, identifier);

        CareOsUserPrincipal principal = CareOsUserPrincipal.fromUser(user);
        String accessToken = jwtTokenService.generateAccessToken(principal, now);
        RefreshTokenService.IssuedRefreshToken refreshToken = refreshTokenService.issueNewFamilyToken(
                user,
                request.isRememberMeEnabled(),
                clientIp,
                userAgent
        );

        cookieService.writeRefreshTokenCookie(
                httpResponse,
                refreshToken.rawToken(),
                refreshToken.ttlSeconds(),
                refreshToken.rememberMe()
        );
        String csrfToken = cookieService.generateAndWriteCsrfCookie(httpResponse);
        httpResponse.setHeader(ApiConstants.CSRF_TOKEN_HEADER, csrfToken);

        log.info("User authenticated successfully: userPublicId={}, scope={}, hospitalId={}",
                principal.getPublicId(), principal.getUserScope(), principal.getHospitalId());

        return new AuthTokenResponse(
                accessToken,
                "Bearer",
                securityProperties.getJwt().getAccessTokenExpirationSeconds(),
                csrfToken,
                authMapper.toAuthUserDto(principal)
        );
    }

    @Transactional(noRollbackFor = CareOsException.class)
    public AuthTokenResponse refresh(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String rawRefreshToken = cookieService.extractRefreshTokenCookie(httpRequest).orElse(null);
        String clientIp = extractClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        try {
            RefreshTokenService.RotatedRefreshToken rotated = refreshTokenService.rotateToken(
                    rawRefreshToken,
                    clientIp,
                    userAgent
            );

            CareOsUserPrincipal principal = userDetailsService.loadPrincipalByPublicId(rotated.userPublicId());
            String accessToken = jwtTokenService.generateAccessToken(principal);

            cookieService.writeRefreshTokenCookie(
                    httpResponse,
                    rotated.issuedToken().rawToken(),
                    rotated.issuedToken().ttlSeconds(),
                    rotated.issuedToken().rememberMe()
            );
            String csrfToken = cookieService.generateAndWriteCsrfCookie(httpResponse);
            httpResponse.setHeader(ApiConstants.CSRF_TOKEN_HEADER, csrfToken);

            return new AuthTokenResponse(
                    accessToken,
                    "Bearer",
                    securityProperties.getJwt().getAccessTokenExpirationSeconds(),
                    csrfToken,
                    authMapper.toAuthUserDto(principal)
            );
        } catch (CareOsException ex) {
            cookieService.clearAuthCookies(httpResponse);
            throw ex;
        }
    }

    @Transactional
    public void logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        cookieService.extractRefreshTokenCookie(httpRequest)
                .ifPresent(token -> refreshTokenService.revokeByRawTokenIfPresent(token, TokenRevocationReason.LOGOUT));
        cookieService.clearAuthCookies(httpResponse);
    }

    @Transactional
    public void logoutAll(HttpServletResponse httpResponse) {
        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        int revokedCount = refreshTokenService.revokeAllForUser(
                principal.getUserId(),
                TokenRevocationReason.LOGOUT_ALL
        );
        cookieService.clearAuthCookies(httpResponse);
        log.info("Revoked {} active refresh token(s) via logout-all for userPublicId={}",
                revokedCount, principal.getPublicId());
    }

    @Transactional(readOnly = true)
    public AuthUserDto getCurrentUser() {
        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        CareOsUserPrincipal fresh = userDetailsService.loadPrincipalByPublicId(principal.getPublicId());
        return authMapper.toAuthUserDto(fresh);
    }

    @Transactional(readOnly = true)
    public UserPermissionsResponse getCurrentPermissions() {
        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        CareOsUserPrincipal fresh = userDetailsService.loadPrincipalByPublicId(principal.getPublicId());
        return authMapper.toPermissionsResponse(fresh);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request, HttpServletResponse httpResponse) {
        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        User user = userRepository.findActiveGraphByPublicId(principal.getPublicId())
                .orElseThrow(() -> new UnauthorizedException("AUTHENTICATION_REQUIRED", "User account unavailable"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("INVALID_CURRENT_PASSWORD", "Current password is incorrect");
        }
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new InvalidRequestException("New password and confirmation password do not match");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new InvalidRequestException("New password must differ from the current password");
        }

        passwordPolicyValidator.validatePasswordStrength(request.newPassword(), user.getUsername());

        Instant now = Instant.now();
        user.updatePasswordHash(passwordEncoder.encode(request.newPassword()), now);
        userRepository.saveAndFlush(user);

        refreshTokenService.revokeAllForUser(user.getId(), TokenRevocationReason.PASSWORD_CHANGED);
        cookieService.clearAuthCookies(httpResponse);

        log.info("Password rotated and active sessions revoked for userPublicId={}", user.getPublicId());
    }

    private boolean isHospitalReadyForLogin(User user) {
        if (user.getUserScope() != UserScope.HOSPITAL) {
            return true;
        }
        return user.getHospital() != null
                && !user.getHospital().isDeleted()
                && user.getHospital().getStatus() == HospitalStatus.ACTIVE;
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
