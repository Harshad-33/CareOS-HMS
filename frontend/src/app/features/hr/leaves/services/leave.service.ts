import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { ApiResponse } from '../../../../core/models/api-response.model';
import { ApiClientService } from '../../../../core/services/api-client.service';
import {
  AdjustLeaveBalanceRequest,
  AllocateLeaveBalanceRequest,
  ApplyLeaveRequest,
  ApproveLeaveStepRequest,
  CreateLeaveTypeRequest,
  LeaveApplication,
  LeaveBalance,
  LeaveBalanceTransaction,
  LeaveCalendarItem,
  LeaveFilterParams,
  LeaveOverview,
  LeaveType,
  PageResponse,
  RejectLeaveStepRequest,
  UpdateLeaveTypeRequest,
} from '../models/leave.models';

@Injectable({
  providedIn: 'root',
})
export class LeaveService {
  private readonly apiClient = inject(ApiClientService);

  // ----------------------------------------------------
  // Management Endpoints (/api/v1/hr/management/leaves)
  // ----------------------------------------------------

  getLeaveTypes(): Observable<LeaveType[]> {
    return this.apiClient
      .get<ApiResponse<LeaveType[]>>('/hr/management/leaves/types')
      .pipe(map((res) => res.data));
  }

  getActiveLeaveTypes(): Observable<LeaveType[]> {
    return this.apiClient
      .get<ApiResponse<LeaveType[]>>('/hr/management/leaves/types/active')
      .pipe(map((res) => res.data));
  }

  createLeaveType(request: CreateLeaveTypeRequest): Observable<LeaveType> {
    return this.apiClient
      .post<ApiResponse<LeaveType>>('/hr/management/leaves/types', request)
      .pipe(map((res) => res.data));
  }

  updateLeaveType(publicId: string, request: UpdateLeaveTypeRequest): Observable<LeaveType> {
    return this.apiClient
      .put<ApiResponse<LeaveType>>(`/hr/management/leaves/types/${publicId}`, request)
      .pipe(map((res) => res.data));
  }

  activateLeaveType(publicId: string): Observable<LeaveType> {
    return this.apiClient
      .patch<ApiResponse<LeaveType>>(`/hr/management/leaves/types/${publicId}/activate`, {})
      .pipe(map((res) => res.data));
  }

  deactivateLeaveType(publicId: string): Observable<LeaveType> {
    return this.apiClient
      .patch<ApiResponse<LeaveType>>(`/hr/management/leaves/types/${publicId}/deactivate`, {})
      .pipe(map((res) => res.data));
  }

  getEmployeeBalances(employeePublicId: string, year?: number): Observable<LeaveBalance[]> {
    const params = year ? { year } : undefined;
    return this.apiClient
      .get<ApiResponse<LeaveBalance[]>>(`/hr/management/leaves/balances/employees/${employeePublicId}`, { params })
      .pipe(map((res) => res.data));
  }

  allocateBalance(request: AllocateLeaveBalanceRequest): Observable<LeaveBalance> {
    return this.apiClient
      .post<ApiResponse<LeaveBalance>>('/hr/management/leaves/balances/allocate', request)
      .pipe(map((res) => res.data));
  }

  adjustBalance(balancePublicId: string, request: AdjustLeaveBalanceRequest): Observable<LeaveBalance> {
    return this.apiClient
      .post<ApiResponse<LeaveBalance>>(`/hr/management/leaves/balances/${balancePublicId}/adjust`, request)
      .pipe(map((res) => res.data));
  }

  getEmployeeTransactions(
    employeePublicId: string,
    page = 0,
    size = 20
  ): Observable<PageResponse<LeaveBalanceTransaction>> {
    return this.apiClient
      .get<ApiResponse<PageResponse<LeaveBalanceTransaction>>>(
        `/hr/management/leaves/balances/employees/${employeePublicId}/transactions`,
        { params: { page, size } }
      )
      .pipe(map((res) => res.data));
  }

  getApplications(params?: LeaveFilterParams): Observable<PageResponse<LeaveApplication>> {
    const queryParams: Record<string, string | number | boolean | null | undefined> = {
      page: params?.page ?? 0,
      size: params?.size ?? 20,
    };
    if (params?.employeePublicId) queryParams['employeePublicId'] = params.employeePublicId;
    if (params?.departmentPublicId) queryParams['departmentPublicId'] = params.departmentPublicId;
    if (params?.leaveTypePublicId) queryParams['leaveTypePublicId'] = params.leaveTypePublicId;
    if (params?.status) queryParams['status'] = params.status;
    if (params?.year) queryParams['year'] = params.year;

    return this.apiClient
      .get<ApiResponse<PageResponse<LeaveApplication>>>('/hr/management/leaves/applications', { params: queryParams })
      .pipe(map((res) => res.data));
  }

  getApplication(publicId: string): Observable<LeaveApplication> {
    return this.apiClient
      .get<ApiResponse<LeaveApplication>>(`/hr/management/leaves/applications/${publicId}`)
      .pipe(map((res) => res.data));
  }

  getPendingApprovals(page = 0, size = 20): Observable<PageResponse<LeaveApplication>> {
    return this.apiClient
      .get<ApiResponse<PageResponse<LeaveApplication>>>('/hr/management/leaves/approvals/pending', {
        params: { page, size },
      })
      .pipe(map((res) => res.data));
  }

  approveStep(
    applicationPublicId: string,
    stepNumber: number,
    request?: ApproveLeaveStepRequest
  ): Observable<LeaveApplication> {
    return this.apiClient
      .post<ApiResponse<LeaveApplication>>(
        `/hr/management/leaves/applications/${applicationPublicId}/steps/${stepNumber}/approve`,
        request ?? {}
      )
      .pipe(map((res) => res.data));
  }

  rejectStep(
    applicationPublicId: string,
    stepNumber: number,
    request: RejectLeaveStepRequest
  ): Observable<LeaveApplication> {
    return this.apiClient
      .post<ApiResponse<LeaveApplication>>(
        `/hr/management/leaves/applications/${applicationPublicId}/steps/${stepNumber}/reject`,
        request
      )
      .pipe(map((res) => res.data));
  }

  getLeaveCalendar(startDate: string, endDate: string, departmentPublicId?: string): Observable<LeaveCalendarItem[]> {
    const params: Record<string, string> = { startDate, endDate };
    if (departmentPublicId) params['departmentPublicId'] = departmentPublicId;
    return this.apiClient
      .get<ApiResponse<LeaveCalendarItem[]>>('/hr/management/leaves/calendar', { params })
      .pipe(map((res) => res.data));
  }

  getLeaveOverview(): Observable<LeaveOverview> {
    return this.apiClient
      .get<ApiResponse<LeaveOverview>>('/hr/management/leaves/overview')
      .pipe(map((res) => res.data));
  }

  downloadApplicationDocument(publicId: string): Observable<Blob> {
    return this.apiClient.getBlob(`/hr/management/leaves/applications/${publicId}/document`);
  }

  // ----------------------------------------------------
  // Self-Service Endpoints (/api/v1/hr/self/leaves)
  // ----------------------------------------------------

  getMyOverview(): Observable<LeaveOverview> {
    return this.apiClient
      .get<ApiResponse<LeaveOverview>>('/hr/self/leaves/overview')
      .pipe(map((res) => res.data));
  }

  getMyBalances(year?: number): Observable<LeaveBalance[]> {
    const params = year ? { year } : undefined;
    return this.apiClient
      .get<ApiResponse<LeaveBalance[]>>('/hr/self/leaves/balances', { params })
      .pipe(map((res) => res.data));
  }

  getMyTransactions(page = 0, size = 20): Observable<PageResponse<LeaveBalanceTransaction>> {
    return this.apiClient
      .get<ApiResponse<PageResponse<LeaveBalanceTransaction>>>('/hr/self/leaves/transactions', {
        params: { page, size },
      })
      .pipe(map((res) => res.data));
  }

  getMyActiveLeaveTypes(): Observable<LeaveType[]> {
    return this.apiClient
      .get<ApiResponse<LeaveType[]>>('/hr/self/leaves/types')
      .pipe(map((res) => res.data));
  }

  applyLeave(request: ApplyLeaveRequest): Observable<LeaveApplication> {
    return this.apiClient
      .post<ApiResponse<LeaveApplication>>('/hr/self/leaves/apply', request)
      .pipe(map((res) => res.data));
  }

  uploadDocument(file: File): Observable<{ documentKey: string; originalFilename: string }> {
    const formData = new FormData();
    formData.append('file', file);
    return this.apiClient
      .post<ApiResponse<{ documentKey: string; originalFilename: string }>>('/hr/self/leaves/upload-document', formData)
      .pipe(map((res) => res.data));
  }

  getMyApplications(page = 0, size = 20): Observable<PageResponse<LeaveApplication>> {
    return this.apiClient
      .get<ApiResponse<PageResponse<LeaveApplication>>>('/hr/self/leaves/applications', {
        params: { page, size },
      })
      .pipe(map((res) => res.data));
  }

  getMyApplication(publicId: string): Observable<LeaveApplication> {
    return this.apiClient
      .get<ApiResponse<LeaveApplication>>(`/hr/self/leaves/applications/${publicId}`)
      .pipe(map((res) => res.data));
  }

  cancelMyApplication(publicId: string): Observable<LeaveApplication> {
    return this.apiClient
      .post<ApiResponse<LeaveApplication>>(`/hr/self/leaves/applications/${publicId}/cancel`, {})
      .pipe(map((res) => res.data));
  }

  downloadMyApplicationDocument(publicId: string): Observable<Blob> {
    return this.apiClient.getBlob(`/hr/self/leaves/applications/${publicId}/document`);
  }
}
