import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { LeaveApplication } from '../../models/leave.models';
import { ApprovalInboxComponent } from './approval-inbox.component';

const MOCK_PENDING_APPS: LeaveApplication[] = [
  {
    publicId: 'app-99',
    applicationNumber: 'LA-2026-0099',
    employeePublicId: 'emp-2',
    employeeName: 'Alice Smith',
    employeeNumber: 'EMP-002',
    departmentName: 'Pediatrics',
    leaveTypePublicId: 'lt-1',
    leaveTypeCode: 'CASUAL',
    leaveTypeName: 'Casual Leave',
    startDate: '2026-09-01',
    endDate: '2026-09-02',
    halfDay: false,
    totalDays: 2,
    reason: 'Family urgent errand',
    hasDocument: false,
    status: 'PENDING',
    appliedAt: '2026-08-20T08:00:00Z',
    currentStepNumber: 1,
    requiredStepsCount: 2,
    approvalSteps: [
      {
        stepNumber: 1,
        stepName: 'Manager Review',
        status: 'PENDING',
      },
      {
        stepNumber: 2,
        stepName: 'HR Final Approval',
        status: 'PENDING',
      },
    ],
  },
];

describe('ApprovalInboxComponent', () => {
  let fixture: ComponentFixture<ApprovalInboxComponent>;
  let component: ApprovalInboxComponent;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApprovalInboxComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should load and render pending leave approvals', () => {
    fixture = TestBed.createComponent(ApprovalInboxComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    const req = httpMock.expectOne((r) => r.url.endsWith('/hr/management/leaves/approvals/pending') && r.method === 'GET');
    req.flush({
      success: true,
      data: {
        content: MOCK_PENDING_APPS,
        totalElements: 1,
        totalPages: 1,
        size: 20,
        number: 0,
      },
    });

    fixture.detectChanges();

    expect(component.applications().length).toBe(1);
    expect(component.isLoading()).toBe(false);

    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('LA-2026-0099');
    expect(el.textContent).toContain('Alice Smith');
    expect(el.textContent).toContain('Step 1 of 2');
    expect(el.textContent).toContain('Approve');
    expect(el.textContent).toContain('Reject');
  });
});
