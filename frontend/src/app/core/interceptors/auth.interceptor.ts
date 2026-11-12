import { HttpContextToken, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { APP_CONSTANTS } from '../constants/app.constants';
import { AuthService } from '../services/auth.service';
import { AuthStateService } from '../services/auth-state.service';
import { NotificationService } from '../services/notification.service';

const RETRIED_WITH_REFRESH = new HttpContextToken<boolean>(() => false);

const PUBLIC_AUTH_ENDPOINTS = ['/auth/login', '/auth/refresh', '/auth/csrf', '/auth/logout'];
const CSRF_ENDPOINTS = ['/auth/refresh', '/auth/logout', '/auth/logout-all'];

function matchesAnyPath(url: string, paths: readonly string[]): boolean {
  return paths.some((path) => url.endsWith(path) || url.includes(`${path}?`));
}

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authState = inject(AuthStateService);
  const authService = inject(AuthService);
  const notificationService = inject(NotificationService);

  const isAuthRoute = req.url.includes('/auth/');
  const isPublicAuthRoute = matchesAnyPath(req.url, PUBLIC_AUTH_ENDPOINTS);
  const isHealthEndpoint = req.url.endsWith('/health') || req.url.includes('/health?');
  const needsCsrf = matchesAnyPath(req.url, CSRF_ENDPOINTS);

  let headers = req.headers;
  const accessToken = authState.accessToken();
  const csrfToken = authState.csrfToken();

  if (accessToken && !isPublicAuthRoute && !isHealthEndpoint) {
    headers = headers.set('Authorization', `Bearer ${accessToken}`);
  }

  if (csrfToken && needsCsrf) {
    headers = headers.set(APP_CONSTANTS.csrfTokenHeader, csrfToken);
  }

  const authRequest = req.clone({
    headers,
    withCredentials: req.withCredentials || isAuthRoute,
  });

  return next(authRequest).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse) {
        if (
          error.status === 401 &&
          !isPublicAuthRoute &&
          !isHealthEndpoint &&
          !authRequest.context.get(RETRIED_WITH_REFRESH)
        ) {
          return authService.refreshToken().pipe(
            switchMap((newAccessToken) => {
              const retriedRequest = authRequest.clone({
                context: authRequest.context.set(RETRIED_WITH_REFRESH, true),
                setHeaders: {
                  Authorization: `Bearer ${newAccessToken}`,
                },
              });
              return next(retriedRequest);
            }),
            catchError((refreshErr) => {
              authService.handleSessionExpired();
              return throwError(() => refreshErr);
            })
          );
        }

        if (error.status === 403 && !isPublicAuthRoute) {
          notificationService.error('You do not have permission to perform this action.');
        }
      }

      return throwError(() => error);
    })
  );
};
