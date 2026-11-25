import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Employee } from '../../models/employee.model';
import {
  EmployeeEditDialogComponent,
  EmployeeEditDialogData,
} from './employee-edit-dialog.component';

const MOCK_EMPLOYEE: Employee = {
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
  hasUserAccount: false,
  createdAt: '2026-01-01T08:00:00Z',
  updatedAt: '2026-01-01T08:00:00Z',
};

const MOCK_DIALOG_DATA: EmployeeEditDialogData = {
  employee: MOCK_EMPLOYEE,
  focusSection: 'personal',
};

describe('EmployeeEditDialogComponent', () => {
  let fixture: ComponentFixture<EmployeeEditDialogComponent>;
  let component: EmployeeEditDialogComponent;
  let httpMock: HttpTestingController;
  let dialogRefSpy: { close: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    dialogRefSpy = { close: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [EmployeeEditDialogComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: MAT_DIALOG_DATA, useValue: MOCK_DIALOG_DATA },
        { provide: MatDialogRef, useValue: dialogRefSpy },
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(EmployeeEditDialogComponent);
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

    // 3. Manager options request
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
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should initialize form with employee values', () => {
    expect(component).toBeTruthy();
    expect(component.form.get('firstName')?.value).toBe('Jane');
    expect(component.form.get('lastName')?.value).toBe('Doe');
    expect(component.form.get('designation')?.value).toBe('Senior Cardiologist');
    expect(component.form.get('departmentPublicId')?.value).toBe('dept-1');
    expect(component.departments().length).toBe(1);
    expect(component.branches().length).toBe(1);
  });

  it('should close dialog with updated request on save', () => {
    component.form.patchValue({
      designation: 'Lead Cardiologist',
      phoneNumber: '+1-555-999-8888',
    });

    component.save();
    expect(dialogRefSpy.close).toHaveBeenCalled();
    const result = dialogRefSpy.close.mock.calls[0][0];
    expect(result.designation).toBe('Lead Cardiologist');
    expect(result.phoneNumber).toBe('+1-555-999-8888');
  });
});
