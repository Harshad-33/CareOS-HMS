# CareOS-HMS Security Architecture (Phase 2)

## 1. Defense-in-Depth Layers
CareOS-HMS enforces security across transport, browser, filter chain, service boundary, and persistence layers:

1. **Spring Security Filter Chain (`SecurityConfig`):**
   - `SessionCreationPolicy.STATELESS` — HTTP sessions are never created or queried (`csrf` managed explicitly for auth cookies via `AuthCookieCsrfFilter`).
   - `AuthCookieCsrfFilter` executes before `JwtAuthenticationFilter` to validate allowed `Origin`/`Referer` and double-submit `X-CSRF-Token` headers on `POST /api/v1/auth/refresh` and cookie-bearing `POST /api/v1/auth/logout`.
   - `JwtAuthenticationFilter` extracts `Bearer` JWTs, verifies HS256 signature and `iss` claim (`JwtTokenService`), reloads current account & tenant state (`CareOsUserDetailsService`), rejects inactive/locked accounts or tokens invalidated by password changes (`pwd_ts`), and populates `SecurityContextHolder`.
   - Deny-by-default authorization: only `OPTIONS`, `/api/v1/health`, `/api/v1/auth/csrf`, `/api/v1/auth/login`, `/api/v1/auth/refresh`, `/api/v1/auth/logout`, and dev-only Swagger paths (`/swagger-ui/**`, `/v3/api-docs/**` when `SWAGGER_ENABLED=true`) are `permitAll()`. All other endpoints require `.authenticated()` plus method-level `@PreAuthorize`.

2. **Password Security & Account Lockout:**
   - Passwords hashed exclusively with `BCryptPasswordEncoder` (strength `12` in dev/prod, `4` in automated test profile).
   - `PasswordPolicyValidator` enforces 12–128 characters containing uppercase, lowercase, numeric, and special characters while disallowing the user's username.
   - Constant-time dummy BCrypt verification prevents timing-based username/email enumeration.
   - Per-account brute-force protection locks accounts for `15` minutes (`423 ACCOUNT_LOCKED`) after `5` consecutive invalid passwords.
   - Per-source sliding-window rate limiting (`LoginRateLimiterService`) throttles rapid bursts per `(clientIp, identifier)` with `429 TOO_MANY_REQUESTS`.

3. **Security HTTP Headers & CORS:**
   - `X-Content-Type-Options: nosniff`
   - `X-Frame-Options: DENY`
   - `Referrer-Policy: strict-origin-when-cross-origin`
   - Cache-control headers preventing caching of authenticated responses
   - Strict CORS allowlist driven by `CORS_ALLOWED_ORIGINS` with `allowCredentials = true`, exposing `X-Correlation-Id` and `X-CSRF-Token`. Wildcard (`*`) origins are never permitted with credentials.

4. **Standardized Security Error Responses:**
   - `CareOsAuthenticationEntryPoint` (`401`) and `CareOsAccessDeniedHandler` (`403`) emit CareOS-HMS `ApiErrorResponse` JSON enriched with `correlationId` (`MdcCorrelationFilter`) and without stack traces or internal user state leakage.

## 2. Database Security Schema (`V2__create_security_tables.sql`)
- `hospitals`: Minimal multi-hospital tenant anchor (`id`, `public_id`, `hospital_code`, `name`, `status`).
- `users`: Supports `PLATFORM` and `HOSPITAL` scopes with DB/JPA invariants (`hospital_id` required for `HOSPITAL`, `NULL` for `PLATFORM`), normalized login identifiers, BCrypt `password_hash`, lockout counters, `password_changed_at`, and audit/soft-delete columns.
- `roles` & `permissions`: Scope-tagged authority definitions (`PLATFORM`, `HOSPITAL`, `COMMON`).
- `user_roles` & `role_permissions`: Normalized join tables avoiding duplicate bindings via unique constraints.
- `refresh_tokens`: Tracks `token_id`, `family_id`, HMAC-SHA256 `token_hash` (64 hex chars), `expires_at`, `revoked`, `revoked_at`, `revoked_reason`, `replaced_by_token_id`, `issued_ip`, and `user_agent`.

## 3. Environment Secrets & Configuration Hygiene
- Startup validation (`CareOsSecurityProperties.validateConfiguration()`) fails fast outside the `test` profile if `JWT_SECRET` is missing or shorter than 32 bytes.
- Real secrets reside exclusively in untracked local `.env` / deployment secret managers (`.gitignore` excludes `.env`); `.env.example` contains placeholders only.
- Structured logging excludes passwords, JWTs, refresh cookies, and CSRF secrets.
