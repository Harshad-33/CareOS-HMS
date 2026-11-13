import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Department, DepartmentRequest, Hospital, HospitalRegistrationRequest } from '../models/hospital.model';
import { HospitalService } from './hospital.service';

describe('HospitalService', () => {
  let service: HospitalService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), HospitalService],
    });
    service = TestBed.inject(HospitalService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should query hospitals with pagination and filters', () => {
    service.getHospitals('ACTIVE', 'City', 0, 10).subscribe((res) => {
      expect(res.content.length).toBe(1);
      expect(res.totalElements).toBe(1);
    });

    const req = httpMock.expectOne((r) => r.url.endsWith('/hospitals') && r.method === 'GET');
    expect(req.request.params.get('status')).toBe('ACTIVE');
    expect(req.request.params.get('search')).toBe('City');
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('10');

    req.flush({
      success: true,
      message: 'OK',
      timestamp: new Date().toISOString(),
      data: {
        content: [
          {
            id: 1,
            publicId: 'hosp-1',
            hospitalCode: 'HOSP-CITY',
            name: 'City Care Hospital',
            hospitalType: 'GENERAL',
            status: 'ACTIVE',
            addressLine1: 'Main St',
            city: 'Mumbai',
            district: 'Mumbai',
            state: 'Maharashtra',
            country: 'India',
            pinCode: '400001',
            email: 'city@careos.local',
            primaryContactNumber: '+919876543210',
            createdAt: new Date().toISOString(),
            updatedAt: new Date().toISOString(),
          },
        ],
        totalElements: 1,
        page: 0,
        size: 10,
        totalPages: 1,
        first: true,
        last: true,
      },
    });
  });

  it('should register a hospital', () => {
    const payload: HospitalRegistrationRequest = {
      hospitalName: 'Sunrise Clinic',
      hospitalCode: 'HOSP-SUNRISE',
      registrationNumber: 'REG-12345',
      hospitalType: 'CLINIC',
      addressLine1: '123 Sol St',
      city: 'Pune',
      district: 'Pune',
      state: 'Maharashtra',
      country: 'India',
      pinCode: '411001',
      email: 'sunrise@careos.local',
      primaryContactNumber: '+919822334455',
      adminUsername: 'sunrise.admin',
      adminEmail: 'admin@sunrise.local',
      adminPassword: 'Password!123',
      adminFirstName: 'Sunrise',
      adminLastName: 'Admin',
    };

    service.registerHospital(payload).subscribe((res: Hospital) => {
      expect(res.name).toBe('Sunrise Clinic');
      expect(res.hospitalCode).toBe('HOSP-SUNRISE');
    });

    const req = httpMock.expectOne((r) => r.url.endsWith('/hospitals') && r.method === 'POST');
    expect(req.request.body).toEqual(payload);

    req.flush({
      success: true,
      message: 'Hospital registered successfully',
      timestamp: new Date().toISOString(),
      data: {
        id: 2,
        publicId: 'hosp-2',
        hospitalCode: 'HOSP-SUNRISE',
        name: 'Sunrise Clinic',
        hospitalType: 'CLINIC',
        status: 'PENDING',
        addressLine1: '123 Sol St',
        city: 'Pune',
        district: 'Pune',
        state: 'Maharashtra',
        country: 'India',
        pinCode: '411001',
        email: 'sunrise@careos.local',
        primaryContactNumber: '+919822334455',
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      },
    });
  });

  it('should create department for the current hospital', () => {
    const depReq: DepartmentRequest = {
      departmentCode: 'EMERGENCY',
      departmentName: 'Emergency Medicine',
      status: 'ACTIVE',
    };

    service.createMyDepartment(depReq).subscribe((dep: Department) => {
      expect(dep.departmentCode).toBe('EMERGENCY');
    });

    const req = httpMock.expectOne((r) => r.url.endsWith('/departments') && r.method === 'POST');
    expect(req.request.body).toEqual(depReq);

    req.flush({
      success: true,
      message: 'Department created',
      timestamp: new Date().toISOString(),
      data: {
        id: 10,
        publicId: 'dept-10',
        departmentCode: 'EMERGENCY',
        departmentName: 'Emergency Medicine',
        status: 'ACTIVE',
        createdAt: new Date().toISOString(),
      },
    });
  });
});