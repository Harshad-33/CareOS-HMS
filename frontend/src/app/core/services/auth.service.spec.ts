import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { PERMISSIONS } from '../constants/permissions.constants';
import { AuthUser } from '../models/auth.model';
import { AuthStateService } from './auth-state.service';
import { AuthService } from './auth.service';

const MOCK_USER: AuthUser = {
  publicId: 'user-pub-1',
  username: 'super.admin',
  email: 'super@careos.local',
  firstName: 'Platform',
  lastName: 'Admin',
  fullName: 'Platform Admin',
  userScope: 'PLATFORM',
  hospitalId: null,
  hospitalCode: null,
  hospitalName: null,
  status: 'ACTIVE',
  mustChangePassword: false,
  lastLoginAt: null,
  roles: ['PLATFORM_SUPER_ADMIN'],
  permissions: [PERMISSIONS.SYSTEM_HEALTH_VIEW, PERMISSIONS.PLATFORM_HOSPITALS_MANAGE],
};

describe('AuthService', () => {
  let authService: AuthService;
  let authState: AuthStateService;
  let httpMock: HttpTestingController;
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    authService = TestBed.inject(AuthService);
    authState = TestBed.inject(AuthStateService);
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should store in-memory access token and user after login', () => {
    authService
      .login({ usernameOrEmail: 'super.admin', password: 'Password!123', rememberMe: true })
      .subscribe((res) => {
        expect(res.accessToken).toBe('access-token-1');
      });

    const req = httpMock.expectOne((r) => r.url.endsWith('/auth/login'));
    expect(req.request.method).toBe('POST');
    expect(req.request.withCredentials).toBe(true);

    req.flush({
      success: true,
      message: 'OK',
      timestamp: new Date().toISOString(),
      data: {
        accessToken: 'access-token-1',
        tokenType: 'Bearer',
        expiresInSeconds: 900,
        csrfToken: 'csrf-1',
        user: MOCK_USER,
      },
    });

    expect(authState.isAuthenticated()).toBe(true);
    expect(authState.accessToken()).toBe('access-token-1');
    expect(authState.csrfToken()).toBe('csrf-1');
  });

  it('should deduplicate concurrent refreshToken calls into a single HTTP request', () => {
    authState.setCsrfToken('csrf-initial');

    const results: string[] = [];
    authService.refreshToken().subscribe((token) => results.push(token));
    authService.refreshToken().subscribe((token) => results.push(token));

    const req = httpMock.expectOne((r) => r.url.endsWith('/auth/refresh'));
    expect(req.request.method).toBe('POST');
    expect(req.request.withCredentials).toBe(true);

    req.flush({
      success: true,
      message: 'Refreshed',
      timestamp: new Date().toISOString(),
      data: {
        accessToken: 'rotated-access-token',
        tokenType: 'Bearer',
        expiresInSeconds: 900,
        csrfToken: 'csrf-rotated',
        user: MOCK_USER,
      },
    });

    expect(results).toEqual(['rotated-access-token', 'rotated-access-token']);
    expect(authState.accessToken()).toBe('rotated-access-token');
    expect(authState.csrfToken()).toBe('csrf-rotated');
  });

  it('should clear session and navigate to login on logout', () => {
    authState.setSession('access-1', 'csrf-1', MOCK_USER);

    authService.logout().subscribe();
    const req = httpMock.expectOne((r) => r.url.endsWith('/auth/logout'));
    expect(req.request.method).toBe('POST');
    req.flush({ success: true, message: 'Logged out', timestamp: new Date().toISOString() });

    expect(authState.isAuthenticated()).toBe(false);
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });
});
