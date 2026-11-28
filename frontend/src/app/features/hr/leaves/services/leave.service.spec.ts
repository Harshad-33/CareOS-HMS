import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import {
  AllocateLeaveBalanceRequest,
  ApplyLeaveRequest,
  CreateLeaveTypeRequest,
  LeaveApplication,
  LeaveApprovalInboxItem,
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

  it('should fetch leave types and active leave types via activeOnly param', () => {
    const mockTypes: LeaveType[] = [
      {
        publicId: 'lt-1',
        code: 'CASUAL',
        name: 'Casual Leave',
        annualAllowance: 12,
        carryForwardEnabled: true,
        maxCarryForwardDays: 5,
        medicalCertRequired: false,
        minServiceDaysRequired: 0,
        paid: true,
        approvalWorkflowType: 'MANAGER_THEN_HR',
        approvalLevelsCount: 2,
        status: 'ACTIVE',
      },
    ];

    service.getActiveLeaveTypes().subscribe((data) => {
      expect(data.length).toBe(1);
      expect(data[0].code).toBe('CASUAL');
    });

    const req = httpMock.expectOne(
      (r) =>
        r.url.endsWith('/hr/management/leaves/types') &&
        r.method === 'GET' &&
        r.params.get('activeOnly') === 'true'
    );
    req.flush({ success: true, data: mockTypes });
  });

  it('should create a leave type with backend-aligned payload', () => {
    const newTypeReq: CreateLeaveTypeRequest = {
      code: 'SICK',
      name: 'Sick Leave',
      annualAllowance: 10,
      carryForwardEnabled: false,
      maxCarryForwardDays: 0,
      medicalCertRequired: true,
      minServiceDaysRequired: 0,
      paid: true,
      approvalWorkflowType: 'HR_ONLY',
      approvalLevelsCount: 1,
    };

    service.createLeaveType(newTypeReq).subscribe((data) => {
      expect(data.code).toBe('SICK');
      expect(data.medicalCertRequired).toBe(true);
    });

    const req = httpMock.expectOne((r) => r.url.endsWith('/hr/management/leaves/types') && r.method === 'POST');
    expect(req.request.body).toEqual(newTypeReq);
    req.flush({ success: true, data: { ...newTypeReq, publicId: 'lt-2', status: 'ACTIVE' } });
  });

  it('should allocate leave balance using calendarYear and remarks', () => {
    const allocReq: AllocateLeaveBalanceRequest = {
      employeePublicId: 'emp-1',
      leaveTypePublicId: 'lt-1',
      calendarYear: 2026,
      allocatedDays: 12,
      remarks: 'Annual allocation',
    };

    service.allocateBalance(allocReq).subscribe((data) => {
      expect(data.availableBalance).toBe(12);
      expect(data.calendarYear).toBe(2026);
    });

    const req = httpMock.expectOne(
      (r) => r.url.endsWith('/hr/management/leaves/balances/allocate') && r.method === 'POST'
    );
    expect(req.request.body).toEqual(allocReq);
    req.flush({
      success: true,
      data: {
        publicId: 'lb-1',
        employeePublicId: 'emp-1',
        leaveTypePublicId: 'lt-1',
        calendarYear: 2026,
        totalAllocated: 12,
        availableBalance: 12,
        usedDays: 0,
        pendingApprovalDays: 0,
      } as Partial<LeaveBalance>,
    });
  });

  it('should fetch pending approvals from /approvals/inbox', () => {
    const mockInbox: LeaveApprovalInboxItem[] = [
      {
        approvalStepPublicId: 'step-1',
        applicationPublicId: 'app-1',
        employeePublicId: 'emp-1',
        employeeNumber: 'EMP-001',
        employeeName: 'John Doe',
        departmentName: 'Cardiology',
        leaveTypePublicId: 'lt-1',
        leaveTypeCode: 'CASUAL',
        leaveTypeName: 'Casual Leave',
        paid: true,
        startDate: '2026-07-01',
        endDate: '2026-07-02',
        totalDays: 2,
        halfDay: false,
        reason: 'Personal',
        stepNumber: 1,
        requiredStepsCount: 2,
        approverRole: 'REPORTING_MANAGER',
        stepStatus: 'PENDING',
        appliedAt: '2026-06-20T09:00:00Z',
      },
    ];

    service.getPendingApprovals().subscribe((page) => {
      expect(page.content.length).toBe(1);
      expect(page.content[0].applicationPublicId).toBe('app-1');
    });

    const req = httpMock.expectOne(
      (r) => r.url.endsWith('/hr/management/leaves/approvals/inbox') && r.method === 'GET'
    );
    req.flush({
      success: true,
      data: { content: mockInbox, totalElements: 1, totalPages: 1, size: 20, number: 0 },
    });
  });

  it('should apply for leave and upload document using documentStorageKey', () => {
    const mockFile = new File(['cert'], 'cert.pdf', { type: 'application/pdf' });
    service.uploadDocument(mockFile).subscribe((res) => {
      expect(res.documentStorageKey).toBe('hosp/doc-1.pdf');
    });

    const uploadReq = httpMock.expectOne(
      (r) => r.url.endsWith('/hr/self/leaves/upload-document') && r.method === 'POST'
    );
    uploadReq.flush({
      success: true,
      data: { documentStorageKey: 'hosp/doc-1.pdf', originalFilename: 'cert.pdf' },
    });

    const applyReq: ApplyLeaveRequest = {
      leaveTypePublicId: 'lt-1',
      startDate: '2026-07-01',
      endDate: '2026-07-03',
      halfDay: false,
      reason: 'Vacation',
      contactPhoneDuringLeave: '+919876543210',
      documentStorageKey: 'hosp/doc-1.pdf',
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
        status: 'PENDING',
        totalDays: 3,
        currentStepNumber: 1,
      } as Partial<LeaveApplication>,
    });
  });

  it('should fetch self-service transactions from /balances/transactions and cancel own leave', () => {
    service.getMyTransactions().subscribe((page) => {
      expect(page.totalElements).toBe(0);
    });

    const txReq = httpMock.expectOne(
      (r) => r.url.endsWith('/hr/self/leaves/balances/transactions') && r.method === 'GET'
    );
    txReq.flush({
      success: true,
      data: { content: [], totalElements: 0, totalPages: 0, size: 20, number: 0 },
    });

    service.cancelMyApplication('app-1').subscribe((data) => {
      expect(data.status).toBe('CANCELLED');
    });

    const cancelReq = httpMock.expectOne(
      (r) => r.url.endsWith('/hr/self/leaves/applications/app-1/cancel') && r.method === 'POST'
    );
    cancelReq.flush({
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
