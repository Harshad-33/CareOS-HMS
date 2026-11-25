import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';
import { PERMISSIONS } from '../../../../core/constants/permissions.constants';
import { AuthUser } from '../../../../core/models/auth.model';
import { AuthStateService } from '../../../../core/services/auth-state.service';
import { Employee, EmployeeLifecycleEvent } from '../../models/employee.model';
import { EmployeeProfileComponent } from './employee-profile.component';

const MOCK_HOSPITAL_ADMIN: AuthUser = {
  publicId: 'user-hosp-1',
  username: 'admin.alpha',
  email: 'admin.alpha@careos.local',
  firstName: 'Alpha',
  lastName: 'Admin',
  fullName: 'Alpha Admin',
  userScope: 'HOSPITAL',
  hospitalId: 1,
  hospitalCode: 'HOSP-ALPHA',
  hospitalName: 'Hospital Alpha',
  status: 'ACTIVE',
  mustChangePassword: false,
  lastLoginAt: null,
  roles: ['HOSPITAL_ADMIN'],
  permissions: [
    PERMISSIONS.HOSPITAL_EMPLOYEES_READ,
    PERMISSIONS.HOSPITAL_EMPLOYEES_MANAGE,
    PERMISSIONS.HOSPITAL_EMPLOYEES_STATUS,
    PERMISSIONS.HOSPITAL_EMPLOYEES_ACCOUNT_LINK,
  ],
};

const MOCK_EMPLOYEE_DETAIL: Employee = {
  publicId: 'emp-uuid-1',
  employeeNumber: 'EMP-2026-0001',
  firstName: 'Jane',
  middleName: 'Ann',
  lastName: 'Doe',
  fullName: 'Jane Ann Doe',
  dateOfBirth: '1988-06-12',
  gender: 'FEMALE',
  bloodGroup: 'O_POSITIVE',
  email: 'jane.doe@careos.local',
  phoneNumber: '+1-555-019-2834',
  alternatePhoneNumber: '+1-555-019-0000',
  addressLine1: '123 Medical Center Blvd',
  addressLine2: 'Suite 400',
  city: 'Metropolis',
  district: 'Central',
  state: 'State',
  country: 'Country',
  pinCode: '12345',
  emergencyContactName: 'John Doe',
  emergencyContactRelation: 'Spouse',
  emergencyContactPhone: '+1-555-019-9999',
  designation: 'Senior Cardiologist',
  departmentPublicId: 'dept-1',
  departmentName: 'Cardiology',
  branchPublicId: 'branch-1',
  branchName: 'Main Campus',
  employmentType: 'FULL_TIME',
  status: 'ACTIVE',
  joiningDate: '2026-01-01',
  reportingManagerPublicId: undefined,
  reportingManagerName: undefined,
  profilePhotoKey: undefined,
  hasUserAccount: true,
  userAccount: {
    publicId: 'usr-1',
    username: 'dr.janedoe',
    email: 'jane.doe@careos.local',
    status: 'ACTIVE',
    roleCodes: ['HOSPITAL_ADMIN'],
  },
  createdAt: '2026-01-01T08:00:00Z',
  updatedAt: '2026-01-01T08:00:00Z',
};

const MOCK_LIFECYCLE_EVENTS: EmployeeLifecycleEvent[] = [
  {
    publicId: 'evt-1',
    previousStatus: undefined,
    newStatus: 'ACTIVE',
    reason: 'Initial Employee Onboarding',
    effectiveDate: '2026-01-01',
    actorUsername: 'admin.alpha',
    createdAt: '2026-01-01T08:00:00Z',
  },
];

describe('EmployeeProfileComponent', () => {
  let fixture: ComponentFixture<EmployeeProfileComponent>;
  let component: EmployeeProfileComponent;
  let httpMock: HttpTestingController;
  let authState: AuthStateService;
  let router: Router;
  let dialog: MatDialog;

  let mockDialog: { open: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    mockDialog = {
      open: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [EmployeeProfileComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: MatDialog, useValue: mockDialog },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: {
                get: (key: string) => (key === 'publicId' ? 'emp-uuid-1' : null),
              },
            },
          },
        },
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    authState = TestBed.inject(AuthStateService);
    router = TestBed.inject(Router);
    dialog = TestBed.inject(MatDialog);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  afterEach(() => {
    httpMock.verify();
  });

  function setupComponent(): void {
    authState.setSession('test-token', 'csrf-token', MOCK_HOSPITAL_ADMIN);
    fixture = TestBed.createComponent(EmployeeProfileComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    // 1. Employee Detail request
    const empReq = httpMock.expectOne((r) => r.url.endsWith('/employees/emp-uuid-1') && r.method === 'GET');
    empReq.flush({
      success: true,
      message: 'OK',
      timestamp: new Date().toISOString(),
      path: '/api/v1/employees/emp-uuid-1',
      data: MOCK_EMPLOYEE_DETAIL,
    });

    // 2. Lifecycle History request
    const lifeReq = httpMock.expectOne(
      (r) => r.url.endsWith('/employees/emp-uuid-1/lifecycle-history') && r.method === 'GET'
    );
    lifeReq.flush({
      success: true,
      message: 'OK',
      timestamp: new Date().toISOString(),
      path: '/api/v1/employees/emp-uuid-1/lifecycle-history',
      data: MOCK_LIFECYCLE_EVENTS,
    });

    fixture.detectChanges();
  }

  it('should load employee profile details and render header', () => {
    setupComponent();
    expect(component).toBeTruthy();
    expect(component.employee()).toBeTruthy();
    expect(component.employee()?.fullName).toBe('Jane Ann Doe');
    expect(component.employee()?.employeeNumber).toBe('EMP-2026-0001');
    expect(component.lifecycleEvents().length).toBe(1);
    expect(component.loading()).toBe(false);
  });

  it('should calculate age properly', () => {
    setupComponent();
    const age = component.calculateAge('1990-01-01');
    expect(typeof age).toBe('number');
    expect(age).toBeGreaterThan(30);

    expect(component.calculateAge('')).toBe('—');
    expect(component.calculateAge('invalid-date')).toBe('—');
  });

  it('should open status dialog and update status', () => {
    setupComponent();

    mockDialog.open.mockReturnValue({
      afterClosed: () => of({ status: 'SUSPENDED', reason: 'License review' }),
    } as any);

    component.openStatusDialog();
    expect(mockDialog.open).toHaveBeenCalled();

    // Expect PATCH status update
    const patchReq = httpMock.expectOne(
      (r) => r.url.endsWith('/employees/emp-uuid-1/status') && r.method === 'PATCH'
    );
    expect(patchReq.request.body.status).toBe('SUSPENDED');
    expect(patchReq.request.body.reason).toBe('License review');

    patchReq.flush({
      success: true,
      message: 'Status updated',
      timestamp: new Date().toISOString(),
      path: '/api/v1/employees/emp-uuid-1/status',
      data: { ...MOCK_EMPLOYEE_DETAIL, status: 'SUSPENDED' },
    });

    // Expect reload lifecycle history
    const lifeReq = httpMock.expectOne(
      (r) => r.url.endsWith('/employees/emp-uuid-1/lifecycle-history') && r.method === 'GET'
    );
    lifeReq.flush({
      success: true,
      message: 'OK',
      timestamp: new Date().toISOString(),
      path: '/api/v1/employees/emp-uuid-1/lifecycle-history',
      data: [
        ...MOCK_LIFECYCLE_EVENTS,
        {
          publicId: 'evt-2',
          previousStatus: 'ACTIVE',
          newStatus: 'SUSPENDED',
          reason: 'License review',
          effectiveDate: '2026-01-02',
          actorUsername: 'admin.alpha',
          createdAt: '2026-01-02T08:00:00Z',
        },
      ],
    });

    expect(component.employee()?.status).toBe('SUSPENDED');
  });

  it('should open account dialog and handle unlinking', () => {
    setupComponent();

    mockDialog.open.mockReturnValue({
      afterClosed: () => of({ action: 'unlink' }),
    } as any);

    component.openAccountDialog();
    expect(mockDialog.open).toHaveBeenCalled();

    // Expect DELETE user account
    const delReq = httpMock.expectOne(
      (r) => r.url.endsWith('/employees/emp-uuid-1/user-account') && r.method === 'DELETE'
    );
    delReq.flush({
      success: true,
      message: 'Account unlinked',
      timestamp: new Date().toISOString(),
      path: '/api/v1/employees/emp-uuid-1/user-account',
      data: { ...MOCK_EMPLOYEE_DETAIL, hasUserAccount: false, userAccount: undefined },
    });

    expect(component.employee()?.hasUserAccount).toBe(false);
  });

  it('should handle soft delete confirmation and navigate to /employees', () => {
    setupComponent();

    mockDialog.open.mockReturnValue({
      afterClosed: () => of(true),
    } as any);

    component.confirmDelete();
    expect(mockDialog.open).toHaveBeenCalled();

    const delReq = httpMock.expectOne(
      (r) => r.url.endsWith('/employees/emp-uuid-1') && r.method === 'DELETE'
    );
    delReq.flush({
      success: true,
      message: 'Employee deleted',
      timestamp: new Date().toISOString(),
      path: '/api/v1/employees/emp-uuid-1',
      data: null,
    });

    expect(router.navigate).toHaveBeenCalledWith(['/employees']);
  });

  it('should navigate back to directory', () => {
    setupComponent();
    component.backToDirectory();
    expect(router.navigate).toHaveBeenCalledWith(['/employees']);
  });
});
