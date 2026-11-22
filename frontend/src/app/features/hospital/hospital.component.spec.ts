import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { provideRouter, Router } from '@angular/router';
import { PERMISSIONS } from '../../core/constants/permissions.constants';
import { AuthUser } from '../../core/models/auth.model';
import { AuthStateService } from '../../core/services/auth-state.service';
import { HospitalComponent } from './hospital.component';

const MOCK_PLATFORM_ADMIN: AuthUser = {
  publicId: 'user-plat-1',
  username: 'platform.superadmin',
  email: 'superadmin@careos-hms.local',
  firstName: 'Platform',
  lastName: 'Administrator',
  fullName: 'Platform Administrator',
  userScope: 'PLATFORM',
  hospitalId: null,
  hospitalCode: null,
  hospitalName: null,
  status: 'ACTIVE',
  mustChangePassword: false,
  lastLoginAt: null,
  roles: ['PLATFORM_SUPER_ADMIN'],
  permissions: [PERMISSIONS.PLATFORM_HOSPITALS_MANAGE],
};

const MOCK_HOSPITAL_ADMIN: AuthUser = {
  publicId: 'user-hosp-1',
  username: 'hospital.admin',
  email: 'admin@citycare.local',
  firstName: 'Hospital',
  lastName: 'Admin',
  fullName: 'Hospital Admin',
  userScope: 'HOSPITAL',
  hospitalId: 1,
  hospitalCode: 'HOSP-CITY',
  hospitalName: 'City Care Hospital',
  status: 'ACTIVE',
  mustChangePassword: false,
  lastLoginAt: null,
  roles: ['HOSPITAL_ADMIN'],
  permissions: [PERMISSIONS.HOSPITAL_WORKSPACE_READ],
};

describe('HospitalComponent', () => {
  let fixture: ComponentFixture<HospitalComponent>;
  let component: HospitalComponent;
  let httpMock: HttpTestingController;
  let authState: AuthStateService;
  let router: Router;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HospitalComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    authState = TestBed.inject(AuthStateService);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  afterEach(() => {
    httpMock.verify();
  });

  function setupAsPlatformAdmin(): void {
    authState.setSession('plat-token', 'csrf-plat', MOCK_PLATFORM_ADMIN);
    fixture = TestBed.createComponent(HospitalComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    const req = httpMock.expectOne((r) => r.url.endsWith('/hospitals') && r.method === 'GET');
    req.flush({
      success: true,
      message: 'OK',
      timestamp: new Date().toISOString(),
      data: {
        content: [],
        totalElements: 0,
        page: 0,
        size: 20,
        totalPages: 0,
        first: true,
        last: true,
      },
    });
    fixture.detectChanges();
  }

  function setupAsHospitalAdmin(): void {
    authState.setSession('hosp-token', 'csrf-hosp', MOCK_HOSPITAL_ADMIN);
    fixture = TestBed.createComponent(HospitalComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    // Hospital admin triggers loadHospitalAdminData()
    const myHospReq = httpMock.expectOne((r) => r.url.endsWith('/hospitals/me') && r.method === 'GET');
    myHospReq.flush({
      success: true,
      message: 'OK',
      data: {
        id: 1,
        publicId: 'hosp-1',
        hospitalCode: 'HOSP-CITY',
        name: 'City Care Hospital',
        hospitalType: 'GENERAL',
        status: 'ACTIVE',
        createdAt: new Date().toISOString(),
      },
    });

    const settingsReq = httpMock.expectOne((r) => r.url.endsWith('/hospitals/me/settings') && r.method === 'GET');
    settingsReq.flush({
      success: true,
      message: 'OK',
      data: {
        timezone: 'Asia/Kolkata',
        currency: 'INR',
        dateFormat: 'YYYY-MM-DD',
        timeFormat: '24_HOUR',
        workingDays: 'MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY',
        workingHoursStart: '09:00:00',
        workingHoursEnd: '18:00:00',
        appointmentSlotDurationMinutes: 15,
        maxAdvanceBookingDays: 30,
      },
    });

    const deptReq = httpMock.expectOne((r) => r.url.endsWith('/departments') && r.method === 'GET');
    deptReq.flush({ success: true, message: 'OK', data: [] });

    const branchReq = httpMock.expectOne((r) => r.url.endsWith('/branches') && r.method === 'GET');
    branchReq.flush({ success: true, message: 'OK', data: [] });

    const contactReq = httpMock.expectOne((r) => r.url.endsWith('/hospitals/me/contacts') && r.method === 'GET');
    contactReq.flush({ success: true, message: 'OK', data: [] });

    const docReq = httpMock.expectOne((r) => r.url.endsWith('/hospitals/me/documents') && r.method === 'GET');
    docReq.flush({ success: true, message: 'OK', data: [] });

    const auditReq = httpMock.expectOne((r) => r.url.includes('/audits') && r.method === 'GET');
    auditReq.flush({
      success: true,
      message: 'OK',
      data: { content: [], totalElements: 0, page: 0, size: 50 },
    });

    fixture.detectChanges();
  }

  it('1. should display "Onboard New Hospital" button in page header for Platform Super Admin', () => {
    setupAsPlatformAdmin();

    const onboardBtn = fixture.debugElement.query(
      By.css('.page-header__actions button[routerLink="/hospital/register"]')
    );
    expect(onboardBtn).toBeTruthy();
    expect(onboardBtn.nativeElement.textContent).toContain('Onboard New Hospital');
    expect(onboardBtn.attributes['page-actions']).toBeDefined();
  });

  it('2. should NOT display "Onboard New Hospital" button for non-platform users', () => {
    setupAsHospitalAdmin();

    const onboardBtn = fixture.debugElement.query(
      By.css('button[routerLink="/hospital/register"]')
    );
    expect(onboardBtn).toBeNull();
  });

  it('3. should navigate to /hospital/register when navigateToRegister is invoked', () => {
    setupAsPlatformAdmin();

    component.navigateToRegister();
    expect(router.navigate).toHaveBeenCalledWith(['/hospital/register']);
  });

  it('4. should render empty-state action button and navigate to /hospital/register when hospital list is empty', () => {
    setupAsPlatformAdmin();

    const emptyState = fixture.debugElement.query(By.css('app-empty-state'));
    expect(emptyState).toBeTruthy();

    const emptyStateBtn = emptyState.query(By.css('button'));
    expect(emptyStateBtn).toBeTruthy();
    expect(emptyStateBtn.nativeElement.textContent).toContain('Onboard First Hospital');

    emptyStateBtn.nativeElement.click();
    expect(router.navigate).toHaveBeenCalledWith(['/hospital/register']);
  });
});