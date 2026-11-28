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
  LeaveApplicationSummary,
  LeaveApprovalInboxItem,
  LeaveBalance,
  LeaveBalanceTransaction,
  LeaveCalendarItem,
  LeaveFilterParams,
  LeaveOverview,
  LeaveType,
  LeaveTypeSummary,
  PageResponse,
  RejectLeaveStepRequest,
  UpdateLeaveTypeRequest,
  UploadDocumentResponse,
} from '../models/leave.models';

@Injectable({
  providedIn: 'root',
})
export class LeaveService {
  private readonly apiClient = inject(ApiClientService);

  // ----------------------------------------------------
  // Management Endpoints (/api/v1/hr/management/leaves)
  // ----------------------------------------------------

  getLeaveTypes(activeOnly = false): Observable<LeaveType[]> {
    return this.apiClient
      .get<ApiResponse<LeaveType[]>>('/hr/management/leaves/types', {
        params: { activeOnly },
      })
      .pipe(map((res) => res.data));
  }

  getActiveLeaveTypes(): Observable<LeaveType[]> {
    return this.getLeaveTypes(true);
  }

  getLeaveTypeSummaries(): Observable<LeaveTypeSummary[]> {
    return this.apiClient
      .get<ApiResponse<LeaveTypeSummary[]>>('/hr/management/leaves/types/summary')
      .pipe(map((res) => res.data));
  }

  getLeaveType(publicId: string): Observable<LeaveType> {
    return this.apiClient
      .get<ApiResponse<LeaveType>>(`/hr/management/leaves/types/${publicId}`)
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

  getApplications(params?: LeaveFilterParams): Observable<PageResponse<LeaveApplicationSummary>> {
    const queryParams: Record<string, string | number | boolean | null | undefined> = {
      page: params?.page ?? 0,
      size: params?.size ?? 20,
    };
    if (params?.status) queryParams['status'] = params.status;

    return this.apiClient
      .get<ApiResponse<PageResponse<LeaveApplicationSummary>>>('/hr/management/leaves/applications', {
        params: queryParams,
      })
      .pipe(map((res) => res.data));
  }

  getApplication(publicId: string): Observable<LeaveApplication> {
    return this.apiClient
      .get<ApiResponse<LeaveApplication>>(`/hr/management/leaves/applications/${publicId}`)
      .pipe(map((res) => res.data));
  }

  getPendingApprovals(page = 0, size = 20): Observable<PageResponse<LeaveApprovalInboxItem>> {
    return this.apiClient
      .get<ApiResponse<PageResponse<LeaveApprovalInboxItem>>>('/hr/management/leaves/approvals/inbox', {
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

  cancelApplication(publicId: string): Observable<LeaveApplication> {
    return this.apiClient
      .post<ApiResponse<LeaveApplication>>(`/hr/management/leaves/applications/${publicId}/cancel`, {})
      .pipe(map((res) => res.data));
  }

  getLeaveCalendar(startDate: string, endDate: string): Observable<LeaveCalendarItem[]> {
    return this.apiClient
      .get<ApiResponse<LeaveCalendarItem[]>>('/hr/management/leaves/calendar', {
        params: { startDate, endDate },
      })
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
      .get<ApiResponse<PageResponse<LeaveBalanceTransaction>>>('/hr/self/leaves/balances/transactions', {
        params: { page, size },
      })
      .pipe(map((res) => res.data));
  }

  getMyActiveLeaveTypes(): Observable<LeaveTypeSummary[]> {
    return this.apiClient
      .get<ApiResponse<LeaveTypeSummary[]>>('/hr/self/leaves/types')
      .pipe(map((res) => res.data));
  }

  applyLeave(request: ApplyLeaveRequest): Observable<LeaveApplication> {
    return this.apiClient
      .post<ApiResponse<LeaveApplication>>('/hr/self/leaves/apply', request)
      .pipe(map((res) => res.data));
  }

  uploadDocument(file: File): Observable<UploadDocumentResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.apiClient
      .post<ApiResponse<UploadDocumentResponse>>('/hr/self/leaves/upload-document', formData)
      .pipe(map((res) => res.data));
  }

  getMyApplications(page = 0, size = 20): Observable<PageResponse<LeaveApplicationSummary>> {
    return this.apiClient
      .get<ApiResponse<PageResponse<LeaveApplicationSummary>>>('/hr/self/leaves/applications', {
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
