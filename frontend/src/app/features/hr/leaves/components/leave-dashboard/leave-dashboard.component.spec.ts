import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { LeaveOverview } from '../../models/leave.models';
import { LeaveDashboardComponent } from './leave-dashboard.component';

const MOCK_OVERVIEW: LeaveOverview = {
  totalApplications: 15,
  pendingApprovals: 3,
  approvedThisMonth: 8,
  employeesOnLeaveToday: 2,
  totalLeaveTypes: 4,
  recentApplications: [
    {
      publicId: 'app-1',
      applicationNumber: 'LA-2026-0001',
      employeePublicId: 'emp-1',
      employeeName: 'John Doe',
      employeeNumber: 'EMP-001',
      departmentName: 'Cardiology',
      leaveTypePublicId: 'lt-1',
      leaveTypeCode: 'CASUAL',
      leaveTypeName: 'Casual Leave',
      startDate: '2026-08-01',
      endDate: '2026-08-03',
      halfDay: false,
      totalDays: 3,
      reason: 'Personal family event',
      hasDocument: false,
      status: 'PENDING',
      appliedAt: '2026-07-25T10:00:00Z',
      currentStepNumber: 1,
      requiredStepsCount: 2,
      approvalSteps: [],
    },
  ],
};

describe('LeaveDashboardComponent', () => {
  let fixture: ComponentFixture<LeaveDashboardComponent>;
  let component: LeaveDashboardComponent;
  let httpMock: HttpTestingController;
  let router: Router;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LeaveDashboardComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should load and render leave overview metrics', () => {
    fixture = TestBed.createComponent(LeaveDashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    const req = httpMock.expectOne((r) => r.url.endsWith('/hr/management/leaves/overview') && r.method === 'GET');
    req.flush({ success: true, data: MOCK_OVERVIEW });
    fixture.detectChanges();

    expect(component.overview()?.pendingApprovals).toBe(3);
    expect(component.overview()?.employeesOnLeaveToday).toBe(2);
    expect(component.overview()?.recentApplications.length).toBe(1);

    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('Pending Approvals');
    expect(el.textContent).toContain('On Leave Today');
    expect(el.textContent).toContain('LA-2026-0001');
  });

  it('should navigate to approval inbox when clicking pending approvals card', () => {
    fixture = TestBed.createComponent(LeaveDashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    const req = httpMock.expectOne((r) => r.url.endsWith('/hr/management/leaves/overview') && r.method === 'GET');
    req.flush({ success: true, data: MOCK_OVERVIEW });

    component.router.navigate(['/hr/leaves/inbox']);
    expect(router.navigate).toHaveBeenCalledWith(['/hr/leaves/inbox']);
  });
});
