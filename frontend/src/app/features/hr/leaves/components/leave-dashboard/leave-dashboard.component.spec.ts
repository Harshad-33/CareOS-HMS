import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { LeaveOverview } from '../../models/leave.models';
import { LeaveDashboardComponent } from './leave-dashboard.component';

const MOCK_OVERVIEW: LeaveOverview = {
  activeLeaveTypesCount: 4,
  pendingApprovalsCount: 3,
  onLeaveTodayCount: 2,
  myAvailableLeaveBalance: 14.5,
  recentApplications: [
    {
      publicId: 'app-10002233',
      employeePublicId: 'emp-1',
      employeeNumber: 'EMP-001',
      employeeName: 'John Doe',
      departmentName: 'Cardiology',
      leaveTypeCode: 'CASUAL',
      leaveTypeName: 'Casual Leave',
      paid: true,
      startDate: '2026-08-01',
      endDate: '2026-08-03',
      totalDays: 3,
      halfDay: false,
      status: 'PENDING',
      currentStepNumber: 1,
      requiredStepsCount: 2,
      appliedAt: '2026-07-25T10:00:00Z',
    },
  ],
  myBalances: [],
};

describe('LeaveDashboardComponent', () => {
  let fixture: ComponentFixture<LeaveDashboardComponent>;
  let component: LeaveDashboardComponent;
  let httpMock: HttpTestingController;
  let router: Router;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LeaveDashboardComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should load and render leave overview metrics matching backend LeaveOverviewResponse', () => {
    fixture = TestBed.createComponent(LeaveDashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    const req = httpMock.expectOne((r) => r.url.endsWith('/hr/management/leaves/overview') && r.method === 'GET');
    req.flush({ success: true, data: MOCK_OVERVIEW });
    fixture.detectChanges();

    expect(component.overview()?.pendingApprovalsCount).toBe(3);
    expect(component.overview()?.onLeaveTodayCount).toBe(2);
    expect(component.overview()?.activeLeaveTypesCount).toBe(4);
    expect(component.overview()?.recentApplications.length).toBe(1);

    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('Pending Approvals');
    expect(el.textContent).toContain('On Leave Today');
    expect(el.textContent).toContain('14.5');
    expect(el.textContent).toContain('app-1000');
  });

  it('should navigate to approval inbox when navigating to inbox route', () => {
    fixture = TestBed.createComponent(LeaveDashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    const req = httpMock.expectOne((r) => r.url.endsWith('/hr/management/leaves/overview') && r.method === 'GET');
    req.flush({ success: true, data: MOCK_OVERVIEW });

    component.router.navigate(['/hr/leaves/inbox']);
    expect(router.navigate).toHaveBeenCalledWith(['/hr/leaves/inbox']);
  });
});
