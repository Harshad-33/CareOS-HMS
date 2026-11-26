import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import {
  AllocateLeaveBalanceRequest,
  ApplyLeaveRequest,
  CreateLeaveTypeRequest,
  LeaveApplication,
  LeaveBalance,
  LeaveType,
} from '../models/leave.models';
import { LeaveService } from './leave.service';

describe('LeaveService', () => {
  let service: LeaveService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), LeaveService],
    });
    service = TestBed.inject(LeaveService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should fetch leave types', () => {
    const mockTypes: LeaveType[] = [
      {
        publicId: 'lt-1',
        code: 'CASUAL',
        name: 'Casual Leave',
        annualAllowance: 12,
        carryForwardAllowed: true,
        maxCarryForwardDays: 5,
        requiresDocument: false,
        maxConsecutiveDays: 3,
        allowHalfDay: true,
        approvalWorkflow: 'MANAGER_THEN_HR',
        approvalLevelsCount: 2,
        status: 'ACTIVE',
      },
    ];

    service.getLeaveTypes().subscribe((data) => {
      expect(data.length).toBe(1);
      expect(data[0].code).toBe('CASUAL');
    });

    const req = httpMock.expectOne((r) => r.url.endsWith('/hr/management/leaves/types') && r.method === 'GET');
    req.flush({ success: true, data: mockTypes });
  });

  it('should create a leave type', () => {
    const newTypeReq: CreateLeaveTypeRequest = {
      code: 'SICK',
      name: 'Sick Leave',
      annualAllowance: 10,
      carryForwardAllowed: false,
      maxCarryForwardDays: 0,
      requiresDocument: true,
      maxConsecutiveDays: 7,
      allowHalfDay: true,
      approvalWorkflow: 'HR_ONLY',
      approvalLevelsCount: 1,
    };

    service.createLeaveType(newTypeReq).subscribe((data) => {
      expect(data.code).toBe('SICK');
      expect(data.requiresDocument).toBe(true);
    });

    const req = httpMock.expectOne((r) => r.url.endsWith('/hr/management/leaves/types') && r.method === 'POST');
    expect(req.request.body).toEqual(newTypeReq);
    req.flush({ success: true, data: { ...newTypeReq, publicId: 'lt-2', status: 'ACTIVE' } });
  });

  it('should allocate leave balance', () => {
    const allocReq: AllocateLeaveBalanceRequest = {
      employeePublicId: 'emp-1',
      leaveTypePublicId: 'lt-1',
      year: 2026,
      allocatedDays: 12,
    };

    service.allocateBalance(allocReq).subscribe((data) => {
      expect(data.availableBalance).toBe(12);
      expect(data.year).toBe(2026);
    });

    const req = httpMock.expectOne(
      (r) => r.url.endsWith('/hr/management/leaves/balances/allocate') && r.method === 'POST'
    );
    req.flush({
      success: true,
      data: {
        publicId: 'lb-1',
        employeePublicId: 'emp-1',
        leaveTypePublicId: 'lt-1',
        year: 2026,
        totalAllocated: 12,
        availableBalance: 12,
        usedDays: 0,
        pendingApprovalDays: 0,
      } as Partial<LeaveBalance>,
    });
  });

  it('should apply for leave in self-service', () => {
    const applyReq: ApplyLeaveRequest = {
      leaveTypePublicId: 'lt-1',
      startDate: '2026-07-01',
      endDate: '2026-07-03',
      halfDay: false,
      reason: 'Vacation',
    };

    service.applyLeave(applyReq).subscribe((data) => {
      expect(data.publicId).toBe('app-1');
      expect(data.status).toBe('PENDING');
      expect(data.totalDays).toBe(3);
    });

    const req = httpMock.expectOne((r) => r.url.endsWith('/hr/self/leaves/apply') && r.method === 'POST');
    expect(req.request.body).toEqual(applyReq);
    req.flush({
      success: true,
      data: {
        publicId: 'app-1',
        applicationNumber: 'LA-2026-0001',
        status: 'PENDING',
        totalDays: 3,
        currentStepNumber: 1,
      } as Partial<LeaveApplication>,
    });
  });

  it('should cancel own leave application', () => {
    service.cancelMyApplication('app-1').subscribe((data) => {
      expect(data.status).toBe('CANCELLED');
    });

    const req = httpMock.expectOne(
      (r) => r.url.endsWith('/hr/self/leaves/applications/app-1/cancel') && r.method === 'POST'
    );
    req.flush({
      success: true,
      data: {
        publicId: 'app-1',
        status: 'CANCELLED',
      } as Partial<LeaveApplication>,
    });
  });

  it('should approve leave application step', () => {
    service.approveStep('app-1', 1, { comments: 'Looks good' }).subscribe((data) => {
      expect(data.status).toBe('APPROVED');
    });

    const req = httpMock.expectOne(
      (r) => r.url.endsWith('/hr/management/leaves/applications/app-1/steps/1/approve') && r.method === 'POST'
    );
    req.flush({
      success: true,
      data: {
        publicId: 'app-1',
        status: 'APPROVED',
      } as Partial<LeaveApplication>,
    });
  });
});
