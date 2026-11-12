import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  provideRouter,
  Router,
  RouterStateSnapshot,
  UrlTree,
} from '@angular/router';
import { PERMISSIONS } from '../constants/permissions.constants';
import { AuthUser } from '../models/auth.model';
import { AuthStateService } from '../services/auth-state.service';
import { authGuard, guestGuard, permissionGuard } from './auth.guard';

const MOCK_HOSPITAL_USER: AuthUser = {
  publicId: 'user-pub-1',
  username: 'admin.cgh',
  email: 'admin@cgh.careos.local',
  firstName: 'Hospital',
  lastName: 'Admin',
  fullName: 'Hospital Admin',
  userScope: 'HOSPITAL',
  hospitalId: 5,
  hospitalCode: 'CGH-01',
  hospitalName: 'CareOS General',
  status: 'ACTIVE',
  mustChangePassword: false,
  lastLoginAt: null,
  roles: ['HOSPITAL_ADMIN'],
  permissions: [PERMISSIONS.SYSTEM_HEALTH_VIEW, PERMISSIONS.HOSPITAL_WORKSPACE_READ],
};

describe('Auth Guards', () => {
  let authState: AuthStateService;
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter([])],
    });
    authState = TestBed.inject(AuthStateService);
    router = TestBed.inject(Router);
  });

  it('authGuard should redirect unauthenticated users to /login preserving returnUrl', () => {
    const result = TestBed.runInInjectionContext(() =>
      authGuard({} as ActivatedRouteSnapshot, { url: '/patients' } as RouterStateSnapshot)
    );
    expect(result instanceof UrlTree).toBe(true);
    expect(router.serializeUrl(result as UrlTree)).toBe('/login?returnUrl=%2Fpatients');
  });

  it('guestGuard should redirect authenticated users to /dashboard', () => {
    authState.setSession('token', 'csrf', MOCK_HOSPITAL_USER);
    const result = TestBed.runInInjectionContext(() =>
      guestGuard({} as ActivatedRouteSnapshot, { url: '/login' } as RouterStateSnapshot)
    );
    expect(result instanceof UrlTree).toBe(true);
    expect(router.serializeUrl(result as UrlTree)).toBe('/dashboard');
  });

  it('permissionGuard should allow granted permission and redirect missing permission to /unauthorized', () => {
    authState.setSession('token', 'csrf', MOCK_HOSPITAL_USER);

    const allowedRoute = {
      data: { requiredPermissions: [PERMISSIONS.HOSPITAL_WORKSPACE_READ], requiredScope: 'HOSPITAL' },
    } as unknown as ActivatedRouteSnapshot;
    const allowedResult = TestBed.runInInjectionContext(() =>
      permissionGuard(allowedRoute, { url: '/patients' } as RouterStateSnapshot)
    );
    expect(allowedResult).toBe(true);

    const deniedRoute = {
      data: { requiredPermissions: [PERMISSIONS.PLATFORM_HOSPITALS_MANAGE] },
    } as unknown as ActivatedRouteSnapshot;
    const deniedResult = TestBed.runInInjectionContext(() =>
      permissionGuard(deniedRoute, { url: '/hospital' } as RouterStateSnapshot)
    );
    expect(deniedResult instanceof UrlTree).toBe(true);
    expect(router.serializeUrl(deniedResult as UrlTree)).toBe('/unauthorized');
  });
});
