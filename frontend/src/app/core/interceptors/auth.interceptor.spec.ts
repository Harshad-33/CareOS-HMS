import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { APP_CONSTANTS } from '../constants/app.constants';
import { PERMISSIONS } from '../constants/permissions.constants';
import { AuthUser } from '../models/auth.model';
import { AuthStateService } from '../services/auth-state.service';
import { NotificationService } from '../services/notification.service';
import { authInterceptor } from './auth.interceptor';

const MOCK_USER: AuthUser = {
  publicId: 'user-pub-1',
  username: 'admin.cgh',
  email: 'admin@cgh.careos.local',
  firstName: 'Hospital',
  lastName: 'Admin',
  fullName: 'Hospital Admin',
  userScope: 'HOSPITAL',
  hospitalId: 10,
  hospitalCode: 'CGH-01',
  hospitalName: 'CareOS General',
  status: 'ACTIVE',
  mustChangePassword: false,
  lastLoginAt: null,
  roles: ['HOSPITAL_ADMIN'],
  permissions: [PERMISSIONS.AUTH_ME_READ],
};

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let authState: AuthStateService;
  let notificationService: NotificationService;
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    authState = TestBed.inject(AuthStateService);
    notificationService = TestBed.inject(NotificationService);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should attach Bearer token to protected endpoints and CSRF header to refresh/logout', () => {
    authState.setSession('access-1', 'csrf-1', MOCK_USER);

    http.get('/api/v1/auth/me').subscribe();
    const meReq = httpMock.expectOne('/api/v1/auth/me');
    expect(meReq.request.headers.get('Authorization')).toBe('Bearer access-1');
    expect(meReq.request.withCredentials).toBe(true);
    meReq.flush({ success: true, data: MOCK_USER });

    http.post('/api/v1/auth/logout', {}).subscribe();
    const logoutReq = httpMock.expectOne('/api/v1/auth/logout');
    expect(logoutReq.request.headers.has('Authorization')).toBe(false);
    expect(logoutReq.request.headers.get(APP_CONSTANTS.csrfTokenHeader)).toBe('csrf-1');
    logoutReq.flush({ success: true });
  });

  it('should refresh once on 401 and replay concurrent requests with new access token', () => {
    authState.setSession('expired-access', 'csrf-1', MOCK_USER);

    let resA: unknown;
    let resB: unknown;
    http.get('/api/v1/auth/me').subscribe((val) => (resA = val));
    http.get('/api/v1/auth/permissions').subscribe((val) => (resB = val));

    const firstMe = httpMock.expectOne('/api/v1/auth/me');
    const firstPerms = httpMock.expectOne('/api/v1/auth/permissions');
    firstMe.flush({ status: 401, code: 'TOKEN_EXPIRED', message: 'Expired' }, { status: 401, statusText: 'Unauthorized' });
    firstPerms.flush({ status: 401, code: 'TOKEN_EXPIRED', message: 'Expired' }, { status: 401, statusText: 'Unauthorized' });

    // Single refresh call despite two 401 responses
    const refreshReq = httpMock.expectOne((r) => r.url.endsWith('/auth/refresh'));
    expect(refreshReq.request.headers.get(APP_CONSTANTS.csrfTokenHeader)).toBe('csrf-1');
    refreshReq.flush({
      success: true,
      data: {
        accessToken: 'fresh-access-2',
        tokenType: 'Bearer',
        expiresInSeconds: 900,
        csrfToken: 'csrf-2',
        user: MOCK_USER,
      },
    });

    const retryMe = httpMock.expectOne('/api/v1/auth/me');
    const retryPerms = httpMock.expectOne('/api/v1/auth/permissions');
    expect(retryMe.request.headers.get('Authorization')).toBe('Bearer fresh-access-2');
    expect(retryPerms.request.headers.get('Authorization')).toBe('Bearer fresh-access-2');

    retryMe.flush({ ok: 'me' });
    retryPerms.flush({ ok: 'perms' });

    expect(resA).toEqual({ ok: 'me' });
    expect(resB).toEqual({ ok: 'perms' });
  });

  it('should clear session when refresh fails and show permission message on 403', () => {
    authState.setSession('access-1', 'csrf-1', MOCK_USER);
    const errorSpy = vi.spyOn(notificationService, 'error');

    http.get('/api/v1/protected/admin').subscribe({ error: () => void 0 });
    const forbiddenReq = httpMock.expectOne('/api/v1/protected/admin');
    forbiddenReq.flush({ status: 403, code: 'ACCESS_DENIED', message: 'Forbidden' }, { status: 403, statusText: 'Forbidden' });
    expect(errorSpy).toHaveBeenCalledWith('You do not have permission to perform this action.');
    expect(authState.isAuthenticated()).toBe(true);

    http.get('/api/v1/auth/me').subscribe({ error: () => void 0 });
    const meReq = httpMock.expectOne('/api/v1/auth/me');
    meReq.flush({}, { status: 401, statusText: 'Unauthorized' });

    const refreshReq = httpMock.expectOne((r) => r.url.endsWith('/auth/refresh'));
    refreshReq.flush({}, { status: 401, statusText: 'Unauthorized' });

    expect(authState.isAuthenticated()).toBe(false);
    expect(router.navigate).toHaveBeenCalled();
  });
});
