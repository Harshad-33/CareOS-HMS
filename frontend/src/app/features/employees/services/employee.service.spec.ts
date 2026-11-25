import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import {
  CreateEmployeeRequest,
  Employee,
  EmployeeSummary,
  UpdateEmployeeRequest,
  UpdateEmployeeStatusRequest,
} from '../models/employee.model';
import { EmployeeService } from './employee.service';

describe('EmployeeService', () => {
  let service: EmployeeService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), EmployeeService],
    });
    service = TestBed.inject(EmployeeService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should query employees with filter parameters', () => {
    const mockSummary: EmployeeSummary = {
      publicId: 'emp-123',
      employeeNumber: 'EMP-2026-0001',
      firstName: 'Jane',
      lastName: 'Doe',
      fullName: 'Jane Doe',
      gender: 'FEMALE',
      designation: 'Senior Cardiologist',
      departmentName: 'Cardiology',
      employmentType: 'FULL_TIME',
      status: 'ACTIVE',
      email: 'jane.doe@careos.local',
      phoneNumber: '+1-555-123-4567',
      joiningDate: '2026-01-01',
      hasUserAccount: true,
    };

    service
      .getEmployees({ search: 'Jane', status: 'ACTIVE', page: 0, size: 10 })
      .subscribe((res) => {
        expect(res.content.length).toBe(1);
        expect(res.content[0].employeeNumber).toBe('EMP-2026-0001');
        expect(res.totalElements).toBe(1);
      });

    const req = httpMock.expectOne((r) => r.url.endsWith('/employees') && r.method === 'GET');
    expect(req.request.params.get('search')).toBe('Jane');
    expect(req.request.params.get('status')).toBe('ACTIVE');
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('10');

    req.flush({
      success: true,
      message: 'OK',
      timestamp: new Date().toISOString(),
      path: '/api/v1/employees',
      data: {
        content: [mockSummary],
        totalElements: 1,
        totalPages: 1,
        size: 10,
        number: 0,
      },
    });
  });

  it('should get employee by public ID', () => {
    const mockEmp: Employee = {
      publicId: 'emp-123',
      employeeNumber: 'EMP-2026-0001',
      firstName: 'Jane',
      lastName: 'Doe',
      fullName: 'Jane Doe',
      dateOfBirth: '1990-05-15',
      gender: 'FEMALE',
      email: 'jane.doe@careos.local',
      phoneNumber: '+1-555-123-4567',
      designation: 'Senior Cardiologist',
      employmentType: 'FULL_TIME',
      status: 'ACTIVE',
      joiningDate: '2026-01-01',
      hasUserAccount: false,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };

    service.getEmployeeByPublicId('emp-123').subscribe((res) => {
      expect(res.publicId).toBe('emp-123');
      expect(res.firstName).toBe('Jane');
    });

    const req = httpMock.expectOne((r) => r.url.endsWith('/employees/emp-123') && r.method === 'GET');
    req.flush({
      success: true,
      message: 'OK',
      timestamp: new Date().toISOString(),
      path: '/api/v1/employees/emp-123',
      data: mockEmp,
    });
  });

  it('should create employee', () => {
    const request: CreateEmployeeRequest = {
      firstName: 'John',
      lastName: 'Smith',
      dateOfBirth: '1988-03-20',
      gender: 'MALE',
      email: 'john.smith@careos.local',
      phoneNumber: '+1-555-987-6543',
      designation: 'Physician',
      departmentPublicId: 'dept-1',
      employmentType: 'FULL_TIME',
      joiningDate: '2026-02-01',
    };

    service.createEmployee(request).subscribe((res) => {
      expect(res.employeeNumber).toBe('EMP-2026-0002');
    });

    const req = httpMock.expectOne((r) => r.url.endsWith('/employees') && r.method === 'POST');
    expect(req.request.body).toEqual(request);
    req.flush({
      success: true,
      message: 'Created',
      timestamp: new Date().toISOString(),
      path: '/api/v1/employees',
      data: { ...request, publicId: 'emp-456', employeeNumber: 'EMP-2026-0002', status: 'ACTIVE' },
    });
  });

  it('should update employee status', () => {
    const statusReq: UpdateEmployeeStatusRequest = {
      status: 'SUSPENDED',
      reason: 'Under inquiry',
    };

    service.updateEmployeeStatus('emp-123', statusReq).subscribe((res) => {
      expect(res.status).toBe('SUSPENDED');
    });

    const req = httpMock.expectOne((r) => r.url.endsWith('/employees/emp-123/status') && r.method === 'PATCH');
    expect(req.request.body).toEqual(statusReq);
    req.flush({
      success: true,
      message: 'Updated',
      timestamp: new Date().toISOString(),
      path: '/api/v1/employees/emp-123/status',
      data: { publicId: 'emp-123', status: 'SUSPENDED' },
    });
  });

  it('should upload profile photo', () => {
    const mockFile = new File(['content'], 'avatar.jpg', { type: 'image/jpeg' });

    service.uploadProfilePhoto('emp-123', mockFile).subscribe((res) => {
      expect(res.profilePhotoKey).toBe('photo-key-1');
    });

    const req = httpMock.expectOne((r) => r.url.endsWith('/employees/emp-123/photo') && r.method === 'POST');
    expect(req.request.body instanceof FormData).toBe(true);
    req.flush({
      success: true,
      message: 'Photo updated',
      timestamp: new Date().toISOString(),
      path: '/api/v1/employees/emp-123/photo',
      data: { publicId: 'emp-123', profilePhotoKey: 'photo-key-1' },
    });
  });
});
