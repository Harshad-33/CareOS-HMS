# CareOS-HMS Authentication Architecture (Phase 2)

## 1. Overview
CareOS-HMS uses a **stateless short-lived JWT access token + stateful rotating `HttpOnly` refresh token** architecture paired with **cross-origin CSRF protection** and **strict multi-tenant binding**. This balances high-throughput API authorization with immediate session revocation and defense against XSS and CSRF attacks.

## 2. Token Strategy & Storage Rules

| Credential | Lifetime | Transport / Storage | Revocable Immediately | Purpose |
| :--- | :--- | :--- | :--- | :--- |
| **Access Token (JWT HS256)** | 15 minutes (`900s`, configurable) | HTTP `Authorization: Bearer <token>` header; stored **strictly in Angular in-memory Signals** (`AuthStateService`) | Yes, via `pwd_ts` timestamp invalidation + user active/lock status lookup in `JwtAuthenticationFilter` | Authorizes API requests without browser storage exposure |
| **Refresh Token (`<tokenId>.<secret>`)** | Session: 12 hours (`43200s`)<br>Remember Me: 7 days (`604800s`) | `HttpOnly`, `Path=/api/v1/auth`, `SameSite=Lax` (dev) / `SameSite=None; Secure=true` (prod) cookie (`careos_refresh_token`); **never returned in JSON** | Yes, stored in `refresh_tokens` as an HMAC-SHA256 digest (`token_hash`) | Silently renews access tokens and enables session revocation / logout |
| **CSRF Token** | Bound to auth cookie lifecycle | `HttpOnly` cookie (`careos_csrf_token`) + `X-CSRF-Token` request header (returned in response body/header on `/auth/csrf`, `/auth/login`, `/auth/refresh`) | Rotated on login & refresh | Protects cookie-authenticated endpoints (`POST /auth/refresh`, `POST /auth/logout`) across origins |

### Why Browser Storage (`localStorage` / `sessionStorage`) Is Forbidden for Tokens
- Any script injection (XSS) can exfiltrate tokens from `localStorage` or `sessionStorage`.
- Keeping the access token exclusively in Angular `AuthStateService` memory and keeping the refresh token inside an `HttpOnly` cookie ensures JavaScript never has direct read access to the long-lived session credential.
- On browser reload or tab initialization, Angular runs `provideAppInitializer(() => inject(AuthService).restoreSession())`, calling `GET /api/v1/auth/csrf` followed by `POST /api/v1/auth/refresh` with `withCredentials: true` to transparently rehydrate the in-memory session.

## 3. End-to-End Authentication Flows

### 3.1 Login Flow
1. User submits `usernameOrEmail`, `password`, and `rememberMe` on `/login`.
2. `LoginRateLimiterService` checks sliding-window attempts per `(clientIp, identifier)` and rejects floods with `429 RATE_LIMIT_EXCEEDED`.
3. `AuthenticationService` fetches active user graph (`User` + `Hospital` + `UserRole` + `Role` + `RolePermission` + `Permission`).
   - Unknown accounts execute a constant-time dummy BCrypt comparison to mitigate username enumeration timing attacks.
   - Inactive users (`AccountStatus != ACTIVE`), soft-deleted users, or `HOSPITAL`-scoped users belonging to an inactive/suspended/deleted hospital return a generic `401 INVALID_CREDENTIALS`.
   - Temporarily locked accounts (`locked_until > now` or `AccountStatus.LOCKED`) return `423 ACCOUNT_LOCKED`.
4. Failed password checks increment `failed_login_attempts`; reaching `maxFailedAttempts` (default `5`) locks the account for `lockoutDurationMinutes` (default `15`). Transactional persistence uses `@Transactional(noRollbackFor = CareOsException.class)` so failure counters commit despite throwing `401`/`423`.
5. On success, counters reset, an HS256 JWT is minted (`JwtTokenService`), a new `family_id` refresh token is persisted in `refresh_tokens`, `HttpOnly` refresh & CSRF cookies are written (`CookieService`), and `AuthTokenResponse` (access token, `csrfToken`, `AuthUserDto`) is returned.

### 3.2 Access Token Expiry & Single-Flight Refresh Rotation
1. An API request receives `401 Unauthorized` due to an expired JWT.
2. `authInterceptor` catches the `401` (for non-public endpoints not yet retried) and calls `AuthService.refreshToken()`.
3. Multiple concurrent failing requests share **one** in-flight HTTP call via `shareReplay(1)`.
4. `AuthCookieCsrfFilter` verifies allowed `Origin` / `Referer` and constant-time equality between `X-CSRF-Token` header and `careos_csrf_token` cookie.
5. `RefreshTokenService.rotateToken(...)`:
   - Looks up `HMAC-SHA256(rawToken)` in `refresh_tokens`.
   - **Reuse Detection:** If the presented token is already marked `revoked = true`, the server immediately revokes **every active token sharing `family_id`** with reason `REUSE_DETECTED`, clears cookies, and returns `401 REFRESH_TOKEN_REUSE_DETECTED`.
   - Otherwise, issues a replacement token in the same `family_id`, marks the previous token `revoked = true` (`reason = ROTATED`, `replaced_by_token_id = <newId>`), rotates the CSRF token, and issues a new JWT.
6. `authInterceptor` clones failed requests with the fresh Bearer token and replays them once (`RETRIED_WITH_REFRESH` HttpContextToken). If refresh fails, Angular clears in-memory state and redirects to `/login`.

### 3.3 Password Change & Session Revocation
- `POST /api/v1/auth/logout`: Revokes the presented refresh token (`reason = LOGOUT`) and clears auth cookies.
- `POST /api/v1/auth/logout-all`: Requires authenticated JWT; revokes all active refresh tokens for `user_id` (`reason = LOGOUT_ALL`) and clears cookies.
- `POST /api/v1/auth/change-password`: Verifies `currentPassword`, enforces password complexity (`PasswordPolicyValidator`), updates `password_hash` and `password_changed_at`, revokes all refresh tokens (`reason = PASSWORD_CHANGED`), and clears cookies. `JwtAuthenticationFilter` rejects any pre-existing access token whose `pwd_ts` precedes `password_changed_at` (`401 TOKEN_REVOKED`).

## 4. Super-Admin Bootstrap Strategy
- No plaintext or default admin credentials are committed in migrations (`V3__seed_initial_rbac.sql`) or source code.
- `SuperAdminBootstrapRunner` runs on startup only when `careos.security.bootstrap.enabled=true` (`BOOTSTRAP_ADMIN_ENABLED=true`).
- `SuperAdminBootstrapService` exits without writes if any non-deleted `PLATFORM_SUPER_ADMIN` user already exists, validates required environment inputs and password complexity, encodes the password via BCrypt, and provisions one `PLATFORM`-scoped `User` assigned to `PLATFORM_SUPER_ADMIN`.
- For production deployments, disable `BOOTSTRAP_ADMIN_ENABLED=false` immediately after initial provisioning.
