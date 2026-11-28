import { Location } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { App } from './app';
import { routes } from './app.routes';
import { PERMISSIONS } from './core/constants/permissions.constants';
import { AppShell } from './core/layout/app-shell';
import { AuthUser } from './core/models/auth.model';
import { AuthStateService } from './core/services/auth-state.service';
import { BreadcrumbService } from './core/services/breadcrumb.service';

const HOSPITAL_ADMIN_USER: AuthUser = {
  publicId: 'hosp-admin-1',
  username: 'admin.cgh',
  email: 'admin@cgh.careos.local',
  firstName: 'CareOS',
  lastName: 'Admin',
  fullName: 'CareOS Admin',
  userScope: 'HOSPITAL',
  hospitalId: 1,
  hospitalCode: 'CGH-01',
  hospitalName: 'CareOS General Hospital',
  status: 'ACTIVE',
  mustChangePassword: false,
  lastLoginAt: null,
  roles: ['HOSPITAL_ADMIN'],
  permissions: [
    PERMISSIONS.AUTH_ME_READ,
    PERMISSIONS.AUTH_PERMISSIONS_READ,
    PERMISSIONS.AUTH_PASSWORD_CHANGE,
    PERMISSIONS.SYSTEM_HEALTH_VIEW,
    PERMISSIONS.HOSPITAL_WORKSPACE_READ,
    PERMISSIONS.HOSPITAL_USERS_MANAGE,
    PERMISSIONS.HOSPITAL_ROLES_READ,
    PERMISSIONS.HOSPITAL_SETTINGS_MANAGE,
    PERMISSIONS.HOSPITAL_EMPLOYEES_READ,
    PERMISSIONS.HOSPITAL_HR_SELF_SERVICE,
    PERMISSIONS.HOSPITAL_LEAVES_APPLY,
    PERMISSIONS.HOSPITAL_LEAVES_READ,
    PERMISSIONS.HOSPITAL_LEAVES_APPROVE,
    PERMISSIONS.HOSPITAL_LEAVES_MANAGE,
  ],
};

const EMPLOYEE_SELF_SERVICE_USER: AuthUser = {
  ...HOSPITAL_ADMIN_USER,
  publicId: 'emp-user-1',
  username: 'nurse.john',
  roles: ['STAFF_NURSE'],
  permissions: [
    PERMISSIONS.AUTH_ME_READ,
    PERMISSIONS.SYSTEM_HEALTH_VIEW,
    PERMISSIONS.HOSPITAL_WORKSPACE_READ,
    PERMISSIONS.HOSPITAL_HR_SELF_SERVICE,
    PERMISSIONS.HOSPITAL_LEAVES_APPLY,
  ],
};

const MANAGER_APPROVER_USER: AuthUser = {
  ...HOSPITAL_ADMIN_USER,
  publicId: 'mgr-user-1',
  username: 'mgr.alice',
  roles: ['WARD_MANAGER'],
  permissions: [
    PERMISSIONS.AUTH_ME_READ,
    PERMISSIONS.SYSTEM_HEALTH_VIEW,
    PERMISSIONS.HOSPITAL_WORKSPACE_READ,
    PERMISSIONS.HOSPITAL_HR_SELF_SERVICE,
    PERMISSIONS.HOSPITAL_LEAVES_APPLY,
    PERMISSIONS.HOSPITAL_LEAVES_APPROVE,
  ],
};

const HR_READ_MANAGE_USER: AuthUser = {
  ...HOSPITAL_ADMIN_USER,
  publicId: 'hr-user-1',
  username: 'hr.officer',
  roles: ['HR_OFFICER'],
  permissions: [
    PERMISSIONS.AUTH_ME_READ,
    PERMISSIONS.SYSTEM_HEALTH_VIEW,
    PERMISSIONS.HOSPITAL_WORKSPACE_READ,
    PERMISSIONS.HOSPITAL_HR_SELF_SERVICE,
    PERMISSIONS.HOSPITAL_LEAVES_APPLY,
    PERMISSIONS.HOSPITAL_LEAVES_READ,
    PERMISSIONS.HOSPITAL_LEAVES_MANAGE,
  ],
};

const PLATFORM_SUPER_ADMIN_USER: AuthUser = {
  publicId: 'plat-admin-1',
  username: 'platform.super',
  email: 'super@careos.local',
  firstName: 'Platform',
  lastName: 'SuperAdmin',
  fullName: 'Platform SuperAdmin',
  userScope: 'PLATFORM',
  hospitalId: null,
  hospitalCode: null,
  hospitalName: null,
  status: 'ACTIVE',
  mustChangePassword: false,
  lastLoginAt: null,
  roles: ['PLATFORM_SUPER_ADMIN'],
  permissions: [
    PERMISSIONS.AUTH_ME_READ,
    PERMISSIONS.AUTH_PERMISSIONS_READ,
    PERMISSIONS.AUTH_PASSWORD_CHANGE,
    PERMISSIONS.SYSTEM_HEALTH_VIEW,
    PERMISSIONS.PLATFORM_OVERVIEW_READ,
    PERMISSIONS.PLATFORM_HOSPITALS_MANAGE,
    PERMISSIONS.PLATFORM_AUDIT_READ,
    PERMISSIONS.PLATFORM_HR_READ,
  ],
};

describe('CareOS-HMS Shell, Auth Routing & RBAC Navigation', () => {
  let authState: AuthStateService;
  let router: Router;
  let location: Location;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter(routes), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    authState = TestBed.inject(AuthStateService);
    router = TestBed.inject(Router);
    location = TestBed.inject(Location);
  });

  it('should create the root app', () => {
    const fixture = TestBed.createComponent(App);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should redirect unauthenticated navigation from root to /login', async () => {
    await router.navigateByUrl('/');
    expect(location.path()).toBe('/login?returnUrl=%2Fdashboard');
  });

  it('should render permission-filtered sidebar navigation items for hospital vs platform scope', async () => {
    authState.setSession('jwt-hosp', 'csrf-hosp', HOSPITAL_ADMIN_USER);
    const hospitalFixture = TestBed.createComponent(AppShell);
    await hospitalFixture.whenStable();
    const compiled = hospitalFixture.nativeElement as HTMLElement;

    expect(compiled.querySelector('.shell-brand__name')?.textContent).toContain('CareOS-HMS');
    expect(compiled.querySelectorAll('.sidebar-item').length).toBe(16);
    expect(compiled.querySelector('.shell-header__tenant-chip')?.textContent).toContain(
      'CareOS General Hospital'
    );

    authState.setSession('jwt-plat', 'csrf-plat', PLATFORM_SUPER_ADMIN_USER);
    await hospitalFixture.whenStable();
    expect(compiled.querySelectorAll('.sidebar-item').length).toBe(3);
    expect(compiled.querySelector('.shell-header__tenant-chip')?.textContent).toContain('Platform Scope');
  });

  it('should allow authenticated user to navigate to /dashboard and /patients and block platform admin from hospital-only route', async () => {
    const breadcrumbService = TestBed.inject(BreadcrumbService);
    authState.setSession('jwt-hosp', 'csrf-hosp', HOSPITAL_ADMIN_USER);

    await router.navigateByUrl('/');
    expect(location.path()).toBe('/dashboard');
    expect(breadcrumbService.breadcrumbs()).toEqual([
      { label: 'Workspace Overview', route: '/dashboard' },
    ]);

    await router.navigateByUrl('/patients');
    expect(location.path()).toBe('/patients');
    expect(breadcrumbService.breadcrumbs()).toEqual([{ label: 'Patients', route: '/patients' }]);

    await router.navigateByUrl('/dashboard');
    authState.setSession('jwt-plat', 'csrf-plat', PLATFORM_SUPER_ADMIN_USER);
    await router.navigateByUrl('/patients');
    expect(location.path()).toBe('/unauthorized');
  });

  it('should enforce Phase 5B RBAC across Employee, Manager, HR, and Platform roles', async () => {
    // 1. Employee self-service only: allowed on /hr/self-service/leaves, blocked from /hr/leaves/*
    authState.setSession('jwt-emp', 'csrf-emp', EMPLOYEE_SELF_SERVICE_USER);
    const shellFixture = TestBed.createComponent(AppShell);
    await shellFixture.whenStable();
    const navTextEmp = (shellFixture.nativeElement as HTMLElement).textContent ?? '';
    expect(navTextEmp).toContain('My Leaves');
    expect(navTextEmp).not.toContain('Leave Management');

    await router.navigateByUrl('/hr/self-service/leaves');
    expect(location.path()).toBe('/hr/self-service/leaves');

    await router.navigateByUrl('/hr/leaves/types');
    expect(location.path()).toBe('/unauthorized');

    await router.navigateByUrl('/hr/leaves/inbox');
    expect(location.path()).toBe('/unauthorized');

    // 2. Manager with HOSPITAL_LEAVES_APPROVE only: /hr/leaves redirects to /hr/leaves/inbox; blocked from /types
    authState.setSession('jwt-mgr', 'csrf-mgr', MANAGER_APPROVER_USER);
    await router.navigateByUrl('/hr/leaves');
    expect(location.path()).toBe('/hr/leaves/inbox');

    await router.navigateByUrl('/hr/leaves/types');
    expect(location.path()).toBe('/unauthorized');

    // 3. HR user without HOSPITAL_LEAVES_APPROVE: allowed on /types and /balances, blocked from /inbox
    authState.setSession('jwt-hr', 'csrf-hr', HR_READ_MANAGE_USER);
    await router.navigateByUrl('/hr/leaves/types');
    expect(location.path()).toBe('/hr/leaves/types');

    await router.navigateByUrl('/hr/leaves/inbox');
    expect(location.path()).toBe('/unauthorized');

    // 4. Platform user blocked from hospital leave routes
    authState.setSession('jwt-plat', 'csrf-plat', PLATFORM_SUPER_ADMIN_USER);
    await router.navigateByUrl('/hr/self-service/leaves');
    expect(location.path()).toBe('/unauthorized');

    await router.navigateByUrl('/hr/leaves');
    expect(location.path()).toBe('/unauthorized');
  });
});
