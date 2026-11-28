import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { LeaveType } from '../../models/leave.models';
import { LeaveTypeListComponent } from './leave-type-list.component';

const MOCK_LEAVE_TYPES: LeaveType[] = [
  {
    publicId: 'lt-1',
    code: 'CASUAL',
    name: 'Casual Leave',
    description: 'Paid casual leave',
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
  {
    publicId: 'lt-2',
    code: 'SICK',
    name: 'Sick Leave',
    description: 'Medical sick leave',
    annualAllowance: 10,
    carryForwardEnabled: false,
    maxCarryForwardDays: 0,
    medicalCertRequired: true,
    minServiceDaysRequired: 0,
    paid: true,
    approvalWorkflowType: 'HR_ONLY',
    approvalLevelsCount: 1,
    status: 'ACTIVE',
  },
];

describe('LeaveTypeListComponent', () => {
  let fixture: ComponentFixture<LeaveTypeListComponent>;
  let component: LeaveTypeListComponent;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LeaveTypeListComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should load and display leave policies in table', () => {
    fixture = TestBed.createComponent(LeaveTypeListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    const req = httpMock.expectOne((r) => r.url.endsWith('/hr/management/leaves/types') && r.method === 'GET');
    req.flush({ success: true, data: MOCK_LEAVE_TYPES });
    fixture.detectChanges();

    expect(component.leaveTypes().length).toBe(2);
    expect(component.isLoading()).toBe(false);

    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('CASUAL');
    expect(el.textContent).toContain('Casual Leave');
    expect(el.textContent).toContain('SICK');
    expect(el.textContent).toContain('Medical Cert Required');
  });
});
