# CareOS-HMS Authentication API Reference (Phase 2)

Base path: `/api/v1/auth`

All responses conform to `ApiResponse<T>` (`success`, `message`, `data`, `timestamp`) or `ApiErrorResponse` (`success`, `status`, `error`, `code`, `message`, `path`, `correlationId`, `timestamp`).

---

## 1. `GET /api/v1/auth/csrf`
Initializes the double-submit CSRF cookie and returns the corresponding token for `X-CSRF-Token` headers.
- **Auth:** Public
- **Response (`200 OK`):** Sets `HttpOnly` cookie `careos_csrf_token` (`Path=/api/v1/auth`) and `X-CSRF-Token` response header.
```json
{
  "success": true,
  "message": "CSRF token initialized",
  "data": {
    "csrfToken": "<opaque-token>",
    "headerName": "X-CSRF-Token"
  }
}
```

## 2. `POST /api/v1/auth/login`
Authenticates via username or email and BCrypt password.
- **Auth:** Public (Rate-limited & lockout protected)
- **Request Body:**
```json
{
  "usernameOrEmail": "platform.super",
  "password": "********",
  "rememberMe": false
}
```
- **Response (`200 OK`):** Sets `HttpOnly` cookies `careos_refresh_token` and `careos_csrf_token`, plus returns:
  - `accessToken`, `tokenType` (`Bearer`), `expiresInSeconds` (`900`), `csrfToken`, `user` (`AuthUserDto` — never includes `passwordHash` or refresh secrets).
- **Errors:**
  - `401 INVALID_CREDENTIALS` (bad credentials, inactive user, or inactive hospital)
  - `423 ACCOUNT_LOCKED` (temporary brute-force lockout)
  - `429 RATE_LIMIT_EXCEEDED` (too many login attempts)

## 3. `POST /api/v1/auth/refresh`
Rotates the active refresh token and issues a new short-lived JWT access token.
- **Auth:** `HttpOnly` cookie `careos_refresh_token` + `careos_csrf_token` cookie + `X-CSRF-Token` header + allowlisted `Origin`/`Referer`
- **Response (`200 OK`):** Rotates `careos_refresh_token` and `careos_csrf_token` cookies; returns fresh `AuthTokenResponse`.
- **Errors:**
  - `403 CSRF_TOKEN_INVALID` / `CSRF_ORIGIN_DENIED`
  - `401 MISSING_REFRESH_TOKEN` / `INVALID_REFRESH_TOKEN` / `REFRESH_TOKEN_EXPIRED`
  - `401 REFRESH_TOKEN_REUSE_DETECTED` (triggers full token-family revocation)

## 4. `POST /api/v1/auth/logout`
Revokes the current session's refresh token (if present) and clears authentication cookies.
- **Auth:** Public/cookie-authenticated (CSRF + Origin enforced when refresh cookie is present)
- **Response (`200 OK`):** Clears auth cookies (`Max-Age=0`).

## 5. `POST /api/v1/auth/logout-all`
Revokes all active refresh token families belonging to the authenticated user across all browsers/devices.
- **Auth:** `Authorization: Bearer <accessToken>` (`isAuthenticated()`)
- **Response (`200 OK`):** Clears auth cookies (`Max-Age=0`).

## 6. `GET /api/v1/auth/me`
Returns the server-resolved profile, tenant context, roles, and effective permissions for the authenticated principal.
- **Auth:** `Authorization: Bearer <accessToken>` + permission `AUTH_ME_READ`

## 7. `GET /api/v1/auth/permissions`
Returns the user's scope, hospital association, roles, and granted permission codes.
- **Auth:** `Authorization: Bearer <accessToken>` + permission `AUTH_PERMISSIONS_READ`

## 8. `POST /api/v1/auth/change-password`
Updates the authenticated user's password after verifying current password and policy compliance, revoking all active refresh sessions and invalidating prior JWTs via `password_changed_at`.
- **Auth:** `Authorization: Bearer <accessToken>` + permission `AUTH_PASSWORD_CHANGE`
- **Request Body:**
```json
{
  "currentPassword": "********",
  "newPassword": "********",
  "confirmPassword": "********"
}
```
- **Errors:** `401 INVALID_CURRENT_PASSWORD`, `400 INVALID_REQUEST` (mismatch, reuse of old password, or failed policy check).
