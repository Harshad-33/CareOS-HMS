import { TestBed } from '@angular/core/testing';
import { PERMISSIONS } from '../constants/permissions.constants';
import { AuthUser } from '../models/auth.model';
import { AuthStateService } from './auth-state.service';

const MOCK_HOSPITAL_ADMIN: AuthUser = {
  publicId: 'user-pub-1',
  username: 'admin.cgh',
  email: 'admin@cgh.careos.local',
  firstName: 'Hospital',
  lastName: 'Admin',
  fullName: 'Hospital Admin',
  userScope: 'HOSPITAL',
  hospitalId: 10,
  hospitalCode: 'CGH-01',
  hospitalName: 'CareOS General Hospital',
  status: 'ACTIVE',
  mustChangePassword: false,
  lastLoginAt: null,
  roles: ['HOSPITAL_ADMIN'],
  permissions: [PERMISSIONS.SYSTEM_HEALTH_VIEW, PERMISSIONS.HOSPITAL_WORKSPACE_READ],
};

describe('AuthStateService', () => {
  let service: AuthStateService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(AuthStateService);
  });

  it('should hold tokens in memory only and compute RBAC/scope access checks', () => {
    expect(service.isAuthenticated()).toBe(false);
    expect(service.accessToken()).toBeNull();

    service.setSession('jwt-access-1', 'csrf-token-1', MOCK_HOSPITAL_ADMIN);

    expect(service.isAuthenticated()).toBe(true);
    expect(service.accessToken()).toBe('jwt-access-1');
    expect(service.csrfToken()).toBe('csrf-token-1');
    expect(service.hospitalId()).toBe(10);
    expect(service.hasPermission(PERMISSIONS.HOSPITAL_WORKSPACE_READ)).toBe(true);
    expect(service.hasPermission(PERMISSIONS.PLATFORM_HOSPITALS_MANAGE)).toBe(false);
    expect(service.canAccess([PERMISSIONS.HOSPITAL_WORKSPACE_READ], 'HOSPITAL')).toBe(true);
    expect(service.canAccess([PERMISSIONS.HOSPITAL_WORKSPACE_READ], 'PLATFORM')).toBe(false);

    expect(localStorage.length).toBe(0);
    expect(sessionStorage.length).toBe(0);
  });

  it('should clear session and store optional expiration notice', () => {
    service.setSession('jwt-access-1', 'csrf-token-1', MOCK_HOSPITAL_ADMIN);
    service.clearSession('Session expired');

    expect(service.isAuthenticated()).toBe(false);
    expect(service.accessToken()).toBeNull();
    expect(service.currentUser()).toBeNull();
    expect(service.sessionExpiredMessage()).toBe('Session expired');

    service.clearSessionExpiredMessage();
    expect(service.sessionExpiredMessage()).toBeNull();
  });
});
