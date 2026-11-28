import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { LeaveApprovalInboxItem } from '../../models/leave.models';
import { ApprovalInboxComponent } from './approval-inbox.component';

const MOCK_INBOX_ITEMS: LeaveApprovalInboxItem[] = [
  {
    approvalStepPublicId: 'step-99',
    applicationPublicId: 'app-99001122',
    employeePublicId: 'emp-2',
    employeeNumber: 'EMP-002',
    employeeName: 'Alice Smith',
    departmentName: 'Pediatrics',
    leaveTypePublicId: 'lt-1',
    leaveTypeCode: 'CASUAL',
    leaveTypeName: 'Casual Leave',
    paid: true,
    startDate: '2026-09-01',
    endDate: '2026-09-02',
    totalDays: 2,
    halfDay: false,
    reason: 'Family urgent errand',
    stepNumber: 1,
    requiredStepsCount: 2,
    approverRole: 'REPORTING_MANAGER',
    assignedApproverEmployeePublicId: 'emp-1',
    assignedApproverName: 'Dr. Manager',
    stepStatus: 'PENDING',
    appliedAt: '2026-08-20T08:00:00Z',
  },
];

describe('ApprovalInboxComponent', () => {
  let fixture: ComponentFixture<ApprovalInboxComponent>;
  let component: ApprovalInboxComponent;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApprovalInboxComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should load and render pending leave approvals from /approvals/inbox', () => {
    fixture = TestBed.createComponent(ApprovalInboxComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    const req = httpMock.expectOne(
      (r) => r.url.endsWith('/hr/management/leaves/approvals/inbox') && r.method === 'GET'
    );
    req.flush({
      success: true,
      data: {
        content: MOCK_INBOX_ITEMS,
        totalElements: 1,
        totalPages: 1,
        size: 20,
        number: 0,
      },
    });

    fixture.detectChanges();

    expect(component.items().length).toBe(1);
    expect(component.isLoading()).toBe(false);

    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('app-9900');
    expect(el.textContent).toContain('Alice Smith');
    expect(el.textContent).toContain('Step 1 of 2');
    expect(el.textContent).toContain('Approve');
    expect(el.textContent).toContain('Reject');
  });
});
