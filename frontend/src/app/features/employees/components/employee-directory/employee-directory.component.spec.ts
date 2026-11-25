import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';
import { PERMISSIONS } from '../../../../core/constants/permissions.constants';
import { AuthUser } from '../../../../core/models/auth.model';
import { AuthStateService } from '../../../../core/services/auth-state.service';
import { EmployeeSummary } from '../../models/employee.model';
import { EmployeeDirectoryComponent } from './employee-directory.component';

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

const MOCK_EMPLOYEE: EmployeeSummary = {
  publicId: 'emp-uuid-1',
  employeeNumber: 'EMP-2026-0001',
  firstName: 'Jane',
  lastName: 'Doe',
  fullName: 'Jane Doe',
  gender: 'FEMALE',
  designation: 'Senior Cardiologist',
  departmentPublicId: 'dept-1',
  departmentName: 'Cardiology',
  branchPublicId: 'branch-1',
  branchName: 'Main Campus',
  employmentType: 'FULL_TIME',
  status: 'ACTIVE',
  email: 'jane.doe@careos.local',
  phoneNumber: '+1-555-019-2834',
  joiningDate: '2026-01-01',
  hasUserAccount: true,
  userPublicId: 'usr-1',
  username: 'dr.janedoe',
};

describe('EmployeeDirectoryComponent', () => {
  let fixture: ComponentFixture<EmployeeDirectoryComponent>;
  let component: EmployeeDirectoryComponent;
  let httpMock: HttpTestingController;
  let authState: AuthStateService;
  let router: Router;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EmployeeDirectoryComponent],
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

  function setupComponent(employees: EmployeeSummary[] = [MOCK_EMPLOYEE]): void {
    authState.setSession('test-token', 'csrf-token', MOCK_HOSPITAL_ADMIN);
    fixture = TestBed.createComponent(EmployeeDirectoryComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    // 1. Department request
    const deptReq = httpMock.expectOne((r) => r.url.endsWith('/departments') && r.method === 'GET');
    deptReq.flush({
      success: true,
      message: 'OK',
      timestamp: new Date().toISOString(),
      path: '/api/v1/departments',
      data: [{ id: 1, publicId: 'dept-1', departmentCode: 'CARDIO', departmentName: 'Cardiology', status: 'ACTIVE' }],
    });

    // 2. Employees request
    const empReq = httpMock.expectOne((r) => r.url.endsWith('/employees') && r.method === 'GET');
    empReq.flush({
      success: true,
      message: 'OK',
      timestamp: new Date().toISOString(),
      path: '/api/v1/employees',
      data: {
        content: employees,
        totalElements: employees.length,
        totalPages: 1,
        size: 20,
        number: 0,
      },
    });

    fixture.detectChanges();
  }

  it('should initialize and load employee directory', () => {
    setupComponent();

    expect(component).toBeTruthy();
    expect(component.employees().length).toBe(1);
    expect(component.employees()[0].fullName).toBe('Jane Doe');
    expect(component.departments().length).toBe(1);
    expect(component.totalEmployees()).toBe(1);
    expect(component.canManage()).toBe(true);
    expect(component.canChangeStatus()).toBe(true);
    expect(component.canLinkAccount()).toBe(true);
  });

  it('should filter employees when search is performed', () => {
    setupComponent();

    component.onSearchInput({ target: { value: 'Jane' } } as unknown as Event);

    const req = httpMock.expectOne((r) => r.url.endsWith('/employees') && r.method === 'GET');
    expect(req.request.params.get('search')).toBe('Jane');
    req.flush({
      success: true,
      message: 'OK',
      timestamp: new Date().toISOString(),
      path: '/api/v1/employees',
      data: {
        content: [MOCK_EMPLOYEE],
        totalElements: 1,
        totalPages: 1,
        size: 20,
        number: 0,
      },
    });

    expect(component.searchQuery()).toBe('Jane');
  });

  it('should navigate to employee registration when clicking register', () => {
    setupComponent();

    component.registerEmployee();
    expect(router.navigate).toHaveBeenCalledWith(['/employees/register']);
  });

  it('should navigate to employee profile when viewing profile', () => {
    setupComponent();

    component.viewProfile(MOCK_EMPLOYEE);
    expect(router.navigate).toHaveBeenCalledWith(['/employees', 'emp-uuid-1']);
  });

  it('should reset filters and reload directory', () => {
    setupComponent();

    component.searchQuery.set('Cardio');
    component.filterStatus.set('ACTIVE');
    expect(component.hasActiveFilters()).toBe(true);

    component.resetFilters();

    const req = httpMock.expectOne((r) => r.url.endsWith('/employees') && r.method === 'GET');
    expect(req.request.params.get('search')).toBeNull();
    expect(req.request.params.get('status')).toBeNull();
    req.flush({
      success: true,
      message: 'OK',
      timestamp: new Date().toISOString(),
      path: '/api/v1/employees',
      data: {
        content: [MOCK_EMPLOYEE],
        totalElements: 1,
        totalPages: 1,
        size: 20,
        number: 0,
      },
    });

    expect(component.hasActiveFilters()).toBe(false);
  });

  it('should handle empty employee list', () => {
    setupComponent([]);

    expect(component.employees().length).toBe(0);
    expect(component.totalEmployees()).toBe(0);
  });
});
