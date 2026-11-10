import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { APP_CONSTANTS } from '../constants/app.constants';
import {
  httpFoundationInterceptor,
  NormalizedHttpError,
} from '../interceptors/http-foundation.interceptor';
import { ApiClientService } from './api-client.service';
import { HealthService } from './health.service';
import { LoadingService } from './loading.service';

describe('ApiClientService, HealthService & Interceptor', () => {
  let apiClient: ApiClientService;
  let healthService: HealthService;
  let loadingService: LoadingService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([httpFoundationInterceptor])),
        provideHttpClientTesting(),
      ],
    });

    apiClient = TestBed.inject(ApiClientService);
    healthService = TestBed.inject(HealthService);
    loadingService = TestBed.inject(LoadingService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    loadingService.reset();
  });

  it('attaches correlation header, toggles loading state, and fetches health status', () => {
    expect(loadingService.isLoading()).toBe(false);

    let receivedStatus: string | undefined;
    healthService.getHealth().subscribe((res) => {
      receivedStatus = res.status;
    });

    expect(loadingService.isLoading()).toBe(true);

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/health`);
    expect(req.request.method).toBe('GET');
    expect(req.request.headers.get(APP_CONSTANTS.correlationIdHeader)).toBeTruthy();

    req.flush({
      status: 'UP',
      service: 'careos-hms-backend',
      version: '0.1.0-SNAPSHOT',
      database: 'UP',
      timestamp: '2026-11-10T18:00:00Z',
    });

    expect(receivedStatus).toBe('UP');
    expect(loadingService.isLoading()).toBe(false);
  });

  it('builds filtered query params and normalizes backend error responses', () => {
    let capturedError: NormalizedHttpError | undefined;

    apiClient
      .get('/sample', { params: { page: 0, size: 20, empty: '', omitted: null } })
      .subscribe({
        error: (err: NormalizedHttpError) => {
          capturedError = err;
        },
      });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/sample?page=0&size=20`);
    req.flush(
      {
        success: false,
        status: 400,
        error: 'Bad Request',
        code: 'INVALID_REQUEST',
        message: 'Invalid filter parameter',
        path: '/api/v1/sample',
        correlationId: 'corr-123',
        timestamp: '2026-11-10T18:00:00Z',
      },
      { status: 400, statusText: 'Bad Request' }
    );

    expect(capturedError?.status).toBe(400);
    expect(capturedError?.code).toBe('INVALID_REQUEST');
    expect(capturedError?.message).toBe('Invalid filter parameter');
    expect(capturedError?.correlationId).toBe('corr-123');
    expect(loadingService.isLoading()).toBe(false);
  });
});
