import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { LeaveApplication, LeaveBalance, LeaveType } from '../../models/leave.models';
import { MyLeavesComponent } from './my-leaves.component';

const MOCK_BALANCES: LeaveBalance[] = [
  {
    publicId: 'lb-1',
    employeePublicId: 'emp-1',
    employeeName: 'Jane Doe',
    employeeNumber: 'EMP-001',
    leaveTypePublicId: 'lt-1',
    leaveTypeCode: 'CASUAL',
    leaveTypeName: 'Casual Leave',
    year: 2026,
    totalAllocated: 12,
    carriedForward: 0,
    usedDays: 2,
    pendingApprovalDays: 1,
    availableBalance: 9,
  },
];

const MOCK_APPLICATIONS: LeaveApplication[] = [
  {
    publicId: 'app-1',
    applicationNumber: 'LA-2026-0001',
    employeePublicId: 'emp-1',
    employeeName: 'Jane Doe',
    employeeNumber: 'EMP-001',
    leaveTypePublicId: 'lt-1',
    leaveTypeCode: 'CASUAL',
    leaveTypeName: 'Casual Leave',
    startDate: '2026-08-10',
    endDate: '2026-08-11',
    halfDay: false,
    totalDays: 2,
    reason: 'Personal errands',
    hasDocument: false,
    status: 'PENDING',
    appliedAt: '2026-07-28T09:00:00Z',
    currentStepNumber: 1,
    requiredStepsCount: 2,
    approvalSteps: [],
  },
];

describe('MyLeavesComponent', () => {
  let fixture: ComponentFixture<MyLeavesComponent>;
  let component: MyLeavesComponent;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MyLeavesComponent],
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

  it('should load personal leave balances and applications', () => {
    fixture = TestBed.createComponent(MyLeavesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    const balReq = httpMock.expectOne((r) => r.url.endsWith('/hr/self/leaves/balances') && r.method === 'GET');
    balReq.flush({ success: true, data: MOCK_BALANCES });

    const typesReq = httpMock.expectOne((r) => r.url.endsWith('/hr/self/leaves/types') && r.method === 'GET');
    typesReq.flush({ success: true, data: [] });

    const appsReq = httpMock.expectOne((r) => r.url.endsWith('/hr/self/leaves/applications') && r.method === 'GET');
    appsReq.flush({
      success: true,
      data: {
        content: MOCK_APPLICATIONS,
        totalElements: 1,
        totalPages: 1,
        size: 20,
        number: 0,
      },
    });

    fixture.detectChanges();

    expect(component.balances().length).toBe(1);
    expect(component.balances()[0].availableBalance).toBe(9);
    expect(component.applications().length).toBe(1);
    expect(component.canCancel(MOCK_APPLICATIONS[0])).toBe(true);

    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('Casual Leave');
    expect(el.textContent).toContain('9');
    expect(el.textContent).toContain('LA-2026-0001');
    expect(el.textContent).toContain('PENDING');
  });
});
