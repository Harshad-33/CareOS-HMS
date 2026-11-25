import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { PERMISSIONS } from '../../../../core/constants/permissions.constants';
import { AuthUser } from '../../../../core/models/auth.model';
import { AuthStateService } from '../../../../core/services/auth-state.service';
import { EmployeeRegistrationComponent } from './employee-registration.component';

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
    PERMISSIONS.HOSPITAL_EMPLOYEES_ACCOUNT_LINK,
  ],
};

describe('EmployeeRegistrationComponent', () => {
  let fixture: ComponentFixture<EmployeeRegistrationComponent>;
  let component: EmployeeRegistrationComponent;
  let httpMock: HttpTestingController;
  let authState: AuthStateService;
  let router: Router;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EmployeeRegistrationComponent],
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

  function setupComponent(): void {
    authState.setSession('test-token', 'csrf-token', MOCK_HOSPITAL_ADMIN);
    fixture = TestBed.createComponent(EmployeeRegistrationComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    // 1. Departments request
    const deptReq = httpMock.expectOne((r) => r.url.endsWith('/departments') && r.method === 'GET');
    deptReq.flush({
      success: true,
      message: 'OK',
      timestamp: new Date().toISOString(),
      path: '/api/v1/departments',
      data: [{ id: 1, publicId: 'dept-1', departmentCode: 'CARDIO', departmentName: 'Cardiology', status: 'ACTIVE' }],
    });

    // 2. Branches request
    const branchReq = httpMock.expectOne((r) => r.url.endsWith('/branches') && r.method === 'GET');
    branchReq.flush({
      success: true,
      message: 'OK',
      timestamp: new Date().toISOString(),
      path: '/api/v1/branches',
      data: [{ id: 1, publicId: 'branch-1', branchCode: 'CAMPUS-1', branchName: 'Main Campus', status: 'ACTIVE' }],
    });

    // 3. Manager options (employees) request
    const mgrReq = httpMock.expectOne((r) => r.url.endsWith('/employees') && r.method === 'GET');
    mgrReq.flush({
      success: true,
      message: 'OK',
      timestamp: new Date().toISOString(),
      path: '/api/v1/employees',
      data: {
        content: [],
        totalElements: 0,
        totalPages: 0,
        size: 100,
        number: 0,
      },
    });

    fixture.detectChanges();
  }

  it('should create registration component and initialize form', () => {
    setupComponent();
    expect(component).toBeTruthy();
    expect(component.form).toBeTruthy();
    expect(component.personalGroup).toBeTruthy();
    expect(component.professionalGroup).toBeTruthy();
    expect(component.accountGroup).toBeTruthy();
    expect(component.departments().length).toBe(1);
    expect(component.branches().length).toBe(1);
  });

  it('should validate required fields in personal group', () => {
    setupComponent();
    const pg = component.personalGroup;
    expect(pg.valid).toBe(false);

    expect(pg.get('firstName')?.hasError('required')).toBe(true);
    expect(pg.get('lastName')?.hasError('required')).toBe(true);
    expect(pg.get('dateOfBirth')?.hasError('required')).toBe(true);
    expect(pg.get('email')?.hasError('required')).toBe(true);
    expect(pg.get('phoneNumber')?.hasError('required')).toBe(true);

    pg.patchValue({
      firstName: 'John',
      lastName: 'Smith',
      dateOfBirth: '1985-05-15',
      gender: 'MALE',
      email: 'john.smith@careos.local',
      phoneNumber: '+1-555-123-4567',
    });
    expect(pg.valid).toBe(true);
  });

  it('should toggle auto-generated employee number validation', () => {
    setupComponent();
    const prof = component.professionalGroup;
    expect(prof.get('autoGenerateCode')?.value).toBe(true);
    expect(prof.get('employeeNumber')?.hasError('required')).toBeFalsy();

    // Disable auto-generation
    component.onAutoGenerateCodeToggle(false);
    expect(prof.get('employeeNumber')?.hasError('required')).toBe(true);

    prof.patchValue({ employeeNumber: 'EMP-CUSTOM-001' });
    expect(prof.get('employeeNumber')?.valid).toBe(true);

    // Re-enable auto-generation
    component.onAutoGenerateCodeToggle(true);
    expect(prof.get('employeeNumber')?.hasError('required')).toBeFalsy();
    expect(prof.get('employeeNumber')?.value).toBe('');
  });

  it('should toggle user account provisioning fields and validation', () => {
    setupComponent();
    expect(component.canLinkAccount()).toBe(true);
    const acc = component.accountGroup;
    expect(acc.get('createUserAccount')?.value).toBe(false);

    // Enable account provisioning
    component.onAccountToggle(true);
    expect(acc.get('username')?.hasError('required')).toBe(true);
    expect(acc.get('password')?.hasError('required')).toBe(true);

    acc.patchValue({
      username: 'dr.jsmith',
      password: 'WeakPassword', // Missing special char / complexity
    });
    expect(acc.get('password')?.hasError('pattern')).toBe(true);

    acc.patchValue({
      password: 'StrongPassword1!',
    });
    expect(acc.get('password')?.valid).toBe(true);
    expect(acc.get('username')?.valid).toBe(true);

    // Disable account provisioning
    component.onAccountToggle(false);
    expect(acc.get('username')?.hasError('required')).toBeFalsy();
    expect(acc.get('password')?.hasError('required')).toBeFalsy();
    expect(acc.get('username')?.value).toBe('');
  });

  it('should submit valid employee registration and navigate to /employees', () => {
    setupComponent();

    component.personalGroup.patchValue({
      firstName: 'Jane',
      lastName: 'Doe',
      dateOfBirth: '1990-01-01',
      gender: 'FEMALE',
      email: 'jane.doe@careos.local',
      phoneNumber: '+1-555-987-6543',
    });

    component.professionalGroup.patchValue({
      autoGenerateCode: true,
      designation: 'Staff Physician',
      employmentType: 'FULL_TIME',
      departmentPublicId: 'dept-1',
      branchPublicId: 'branch-1',
      joiningDate: '2026-02-01',
    });

    expect(component.form.valid).toBe(true);

    component.onSubmit();
    expect(component.submitting()).toBe(true);

    const req = httpMock.expectOne((r) => r.url.endsWith('/employees') && r.method === 'POST');
    expect(req.request.body.firstName).toBe('Jane');
    expect(req.request.body.departmentPublicId).toBe('dept-1');
    expect(req.request.body.createUserAccount).toBe(false);

    req.flush({
      success: true,
      message: 'Employee registered successfully',
      timestamp: new Date().toISOString(),
      path: '/api/v1/employees',
      data: {
        publicId: 'emp-new-1',
        employeeNumber: 'EMP-2026-0002',
        firstName: 'Jane',
        lastName: 'Doe',
        fullName: 'Jane Doe',
      },
    });

    expect(component.submitting()).toBe(false);
    expect(router.navigate).toHaveBeenCalledWith(['/employees']);
  });

  it('should navigate back to /employees on cancel', () => {
    setupComponent();
    component.cancel();
    expect(router.navigate).toHaveBeenCalledWith(['/employees']);
  });
});
