import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { PERMISSIONS } from '../../../core/constants/permissions.constants';
import { AuthUser } from '../../../core/models/auth.model';
import { AuthStateService } from '../../../core/services/auth-state.service';
import { LoginComponent } from './login';

const MOCK_USER: AuthUser = {
  publicId: 'pub-1',
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
  permissions: [PERMISSIONS.SYSTEM_HEALTH_VIEW],
};

describe('LoginComponent', () => {
  let httpMock: HttpTestingController;
  let authState: AuthStateService;
  let router: Router;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { queryParamMap: convertToParamMap({ returnUrl: '/hospital' }) },
          },
        },
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    authState = TestBed.inject(AuthStateService);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should require username/email and password before submitting', () => {
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();

    const component = fixture.componentInstance;
    component.onSubmit();

    expect(component.loginForm.invalid).toBe(true);
    httpMock.expectNone((r) => r.url.endsWith('/auth/login'));
  });

  it('should toggle password visibility and display session expiration banner', () => {
    authState.clearSession('Your session has expired.');
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();

    const component = fixture.componentInstance;
    expect(component.hidePassword()).toBe(true);
    component.togglePasswordVisibility();
    expect(component.hidePassword()).toBe(false);

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.login-banner--warning')?.textContent).toContain('Your session has expired.');
  });

  it('should authenticate and navigate to safe returnUrl', () => {
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();

    const component = fixture.componentInstance;
    component.loginForm.setValue({
      usernameOrEmail: 'super.admin',
      password: 'Password!1234',
      rememberMe: true,
    });
    component.onSubmit();

    const req = httpMock.expectOne((r) => r.url.endsWith('/auth/login'));
    expect(req.request.body).toEqual({
      usernameOrEmail: 'super.admin',
      password: 'Password!1234',
      rememberMe: true,
    });

    req.flush({
      success: true,
      message: 'Authenticated',
      timestamp: new Date().toISOString(),
      data: {
        accessToken: 'jwt-1',
        tokenType: 'Bearer',
        expiresInSeconds: 900,
        csrfToken: 'csrf-1',
        user: MOCK_USER,
      },
    });

    expect(authState.isAuthenticated()).toBe(true);
    expect(router.navigateByUrl).toHaveBeenCalledWith('/hospital');
  });
});
