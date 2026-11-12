import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, finalize, firstValueFrom, map, of, shareReplay, switchMap, tap } from 'rxjs';
import { ApiResponse } from '../models/api-response.model';
import {
  AuthTokenResponse,
  AuthUser,
  ChangePasswordRequest,
  CsrfTokenResponse,
  LoginRequest,
  UserPermissionsResponse,
} from '../models/auth.model';
import { ApiClientService } from './api-client.service';
import { AuthStateService } from './auth-state.service';
import { NotificationService } from './notification.service';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly apiClient = inject(ApiClientService);
  private readonly authState = inject(AuthStateService);
  private readonly notificationService = inject(NotificationService);
  private readonly router = inject(Router);

  private refreshInFlight$: Observable<string> | null = null;

  initCsrf(): Observable<CsrfTokenResponse> {
    return this.apiClient
      .get<ApiResponse<CsrfTokenResponse>>('/auth/csrf', { withCredentials: true })
      .pipe(
        map((response) => response.data),
        tap((payload) => this.authState.setCsrfToken(payload.csrfToken))
      );
  }

  login(request: LoginRequest): Observable<AuthTokenResponse> {
    return this.apiClient
      .post<ApiResponse<AuthTokenResponse>, LoginRequest>('/auth/login', request, {
        withCredentials: true,
      })
      .pipe(
        map((response) => response.data),
        tap((payload) => {
          this.authState.setSession(payload.accessToken, payload.csrfToken, payload.user);
        })
      );
  }

  refreshToken(): Observable<string> {
    if (this.refreshInFlight$) {
      return this.refreshInFlight$;
    }

    const ensureCsrf$ = this.authState.csrfToken()
      ? of(this.authState.csrfToken()!)
      : this.initCsrf().pipe(map((res) => res.csrfToken));

    this.refreshInFlight$ = ensureCsrf$.pipe(
      switchMap(() =>
        this.apiClient.post<ApiResponse<AuthTokenResponse>, Record<string, never>>(
          '/auth/refresh',
          {},
          { withCredentials: true }
        )
      ),
      map((response) => response.data),
      tap((payload) => {
        this.authState.setSession(payload.accessToken, payload.csrfToken, payload.user);
      }),
      map((payload) => payload.accessToken),
      shareReplay(1),
      finalize(() => {
        this.refreshInFlight$ = null;
      })
    );

    return this.refreshInFlight$;
  }

  async restoreSession(): Promise<void> {
    try {
      await firstValueFrom(
        this.initCsrf().pipe(
          switchMap(() => this.refreshToken()),
          map(() => void 0),
          catchError(() => {
            this.authState.clearSession();
            return of(void 0);
          })
        )
      );
    } finally {
      this.authState.markInitialized();
    }
  }

  logout(sessionExpiredMessage?: string): Observable<void> {
    return this.apiClient
      .post<ApiResponse<void>, Record<string, never>>('/auth/logout', {}, { withCredentials: true })
      .pipe(
        map(() => void 0),
        catchError(() => of(void 0)),
        tap(() => {
          this.authState.clearSession(sessionExpiredMessage);
          void this.router.navigate(['/login']);
        })
      );
  }

  logoutAll(): Observable<void> {
    return this.apiClient
      .post<ApiResponse<void>, Record<string, never>>('/auth/logout-all', {}, { withCredentials: true })
      .pipe(
        map(() => void 0),
        tap(() => {
          this.authState.clearSession();
          this.notificationService.info('All active sessions have been signed out.');
          void this.router.navigate(['/login']);
        })
      );
  }

  fetchMe(): Observable<AuthUser> {
    return this.apiClient.get<ApiResponse<AuthUser>>('/auth/me').pipe(
      map((response) => response.data),
      tap((user) => this.authState.updateUser(user))
    );
  }

  fetchPermissions(): Observable<UserPermissionsResponse> {
    return this.apiClient
      .get<ApiResponse<UserPermissionsResponse>>('/auth/permissions')
      .pipe(map((response) => response.data));
  }

  changePassword(request: ChangePasswordRequest): Observable<void> {
    return this.apiClient
      .post<ApiResponse<void>, ChangePasswordRequest>('/auth/change-password', request, {
        withCredentials: true,
      })
      .pipe(
        map(() => void 0),
        tap(() => {
          this.authState.clearSession(
            'Password updated successfully. Please sign in with your new password.'
          );
          this.notificationService.info('Password updated. Please sign in again.');
          void this.router.navigate(['/login']);
        })
      );
  }

  handleSessionExpired(returnUrl?: string): void {
    const message = 'Your session has expired. Please sign in again.';
    this.authState.clearSession(message);
    this.notificationService.error(message);

    if (!this.router.url.startsWith('/login')) {
      const queryParams = returnUrl && returnUrl !== '/login' ? { returnUrl } : undefined;
      void this.router.navigate(['/login'], { queryParams });
    }
  }
}
