package com.careos.hms.authentication.controller;

import com.careos.hms.authentication.dto.AuthTokenResponse;
import com.careos.hms.authentication.dto.AuthUserDto;
import com.careos.hms.authentication.dto.ChangePasswordRequest;
import com.careos.hms.authentication.dto.CsrfTokenResponse;
import com.careos.hms.authentication.dto.LoginRequest;
import com.careos.hms.authentication.dto.UserPermissionsResponse;
import com.careos.hms.authentication.service.AuthenticationService;
import com.careos.hms.common.constants.ApiConstants;
import com.careos.hms.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiConstants.API_V1_BASE_PATH + "/auth")
@Tag(name = "Authentication", description = "JWT authentication, refresh token rotation, and user security context endpoints")
public class AuthController {

    private final AuthenticationService authenticationService;

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @GetMapping("/csrf")
    @Operation(
            summary = "Initialize CSRF token for auth cookie operations",
            description = "Issues a double-submit CSRF cookie and returns the corresponding CSRF token header value."
    )
    public ResponseEntity<ApiResponse<CsrfTokenResponse>> getCsrfToken(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        CsrfTokenResponse payload = authenticationService.issueCsrfToken(request, response);
        return ResponseEntity.ok(ApiResponse.ok(payload, "CSRF token initialized", request.getRequestURI()));
    }

    @PostMapping("/login")
    @Operation(
            summary = "Authenticate with username or email",
            description = "Verifies BCrypt password, checks account status/lockout, sets HttpOnly refresh cookie, and returns short-lived JWT access token."
    )
    public ResponseEntity<ApiResponse<AuthTokenResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        AuthTokenResponse payload = authenticationService.login(request, httpRequest, httpResponse);
        return ResponseEntity.ok(ApiResponse.ok(payload, "Authentication successful", httpRequest.getRequestURI()));
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Rotate refresh token and issue new access token",
            description = "Validates HttpOnly refresh cookie and X-CSRF-Token header, performs token rotation with reuse detection, and returns fresh access token."
    )
    public ResponseEntity<ApiResponse<AuthTokenResponse>> refresh(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        AuthTokenResponse payload = authenticationService.refresh(httpRequest, httpResponse);
        return ResponseEntity.ok(ApiResponse.ok(payload, "Token refreshed successfully", httpRequest.getRequestURI()));
    }

    @PostMapping("/logout")
    @Operation(
            summary = "Logout current session",
            description = "Revokes the current refresh token if present and clears authentication cookies."
    )
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        authenticationService.logout(httpRequest, httpResponse);
        return ResponseEntity.ok(ApiResponse.message("Logged out successfully", httpRequest.getRequestURI()));
    }

    @PostMapping("/logout-all")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Logout all sessions for current user",
            description = "Revokes all active refresh tokens belonging to the authenticated user and clears cookies."
    )
    public ResponseEntity<ApiResponse<Void>> logoutAll(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        authenticationService.logoutAll(httpResponse);
        return ResponseEntity.ok(ApiResponse.message("All sessions revoked successfully", httpRequest.getRequestURI()));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('AUTH_ME_READ')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Get authenticated user profile",
            description = "Returns profile, tenant association, assigned roles, and effective permissions for the authenticated user."
    )
    public ResponseEntity<ApiResponse<AuthUserDto>> me(HttpServletRequest httpRequest) {
        AuthUserDto payload = authenticationService.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.ok(payload, "User profile retrieved", httpRequest.getRequestURI()));
    }

    @PostMapping("/change-password")
    @PreAuthorize("hasAuthority('AUTH_PASSWORD_CHANGE')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Change authenticated user's password",
            description = "Verifies current password, enforces password strength policy, updates BCrypt hash, and revokes active refresh sessions."
    )
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        authenticationService.changePassword(request, httpResponse);
        return ResponseEntity.ok(ApiResponse.message(
                "Password changed successfully. Please sign in again.",
                httpRequest.getRequestURI()
        ));
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('AUTH_PERMISSIONS_READ')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Get authenticated user's roles and permissions",
            description = "Returns scope, hospital binding, roles, and granted permission codes."
    )
    public ResponseEntity<ApiResponse<UserPermissionsResponse>> permissions(HttpServletRequest httpRequest) {
        UserPermissionsResponse payload = authenticationService.getCurrentPermissions();
        return ResponseEntity.ok(ApiResponse.ok(payload, "Permissions retrieved", httpRequest.getRequestURI()));
    }
}
