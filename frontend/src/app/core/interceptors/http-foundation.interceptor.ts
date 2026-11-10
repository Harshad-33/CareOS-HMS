import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, finalize, throwError } from 'rxjs';
import { APP_CONSTANTS } from '../constants/app.constants';
import { ApiErrorResponse } from '../models/api-response.model';
import { LoadingService } from '../services/loading.service';

export interface NormalizedHttpError {
  status: number;
  code: string;
  message: string;
  correlationId?: string;
}

export function normalizeHttpError(error: HttpErrorResponse): NormalizedHttpError {
  const payload = error.error as Partial<ApiErrorResponse> | null;
  if (payload && typeof payload === 'object' && typeof payload.message === 'string') {
    return {
      status: error.status || payload.status || 500,
      code: payload.code ?? 'API_ERROR',
      message: payload.message,
      correlationId: payload.correlationId,
    };
  }

  if (error.status === 0) {
    return {
      status: 0,
      code: 'NETWORK_UNREACHABLE',
      message: 'Unable to reach the CareOS-HMS API server. Please verify the backend service is running.',
    };
  }

  return {
    status: error.status,
    code: 'HTTP_ERROR',
    message: error.message || 'An unexpected communication error occurred.',
  };
}

function generateCorrelationId(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }
  return `ui-${Date.now()}-${Math.random().toString(16).slice(2, 10)}`;
}

export const httpFoundationInterceptor: HttpInterceptorFn = (req, next) => {
  const loadingService = inject(LoadingService);
  const correlationId = req.headers.get(APP_CONSTANTS.correlationIdHeader) ?? generateCorrelationId();

  const requestWithCorrelation = req.clone({
    setHeaders: {
      [APP_CONSTANTS.correlationIdHeader]: correlationId,
    },
  });

  loadingService.start();

  return next(requestWithCorrelation).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse) {
        return throwError(() => normalizeHttpError(error));
      }
      return throwError(() => error);
    }),
    finalize(() => loadingService.stop())
  );
};
