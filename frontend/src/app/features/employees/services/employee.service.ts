import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { ApiResponse } from '../../../core/models/api-response.model';
import { ApiClientService } from '../../../core/services/api-client.service';
import {
  CreateEmployeeRequest,
  Employee,
  EmployeeFilterParams,
  EmployeeLifecycleEvent,
  EmployeeSummary,
  LinkUserAccountRequest,
  PageResponse,
  UpdateEmployeeRequest,
  UpdateEmployeeStatusRequest,
} from '../models/employee.model';

@Injectable({
  providedIn: 'root',
})
export class EmployeeService {
  private readonly apiClient = inject(ApiClientService);

  getEmployees(params?: EmployeeFilterParams): Observable<PageResponse<EmployeeSummary>> {
    const queryParams: Record<string, string | number | boolean | null | undefined> = {
      page: params?.page ?? 0,
      size: params?.size ?? 20,
    };
    if (params?.search) queryParams['search'] = params.search;
    if (params?.departmentPublicId) queryParams['departmentPublicId'] = params.departmentPublicId;
    if (params?.branchPublicId) queryParams['branchPublicId'] = params.branchPublicId;
    if (params?.designation) queryParams['designation'] = params.designation;
    if (params?.employmentType) queryParams['employmentType'] = params.employmentType;
    if (params?.status) queryParams['status'] = params.status;

    return this.apiClient
      .get<ApiResponse<PageResponse<EmployeeSummary>>>('/employees', { params: queryParams })
      .pipe(map((res) => res.data));
  }

  getEmployeeByPublicId(publicId: string): Observable<Employee> {
    return this.apiClient
      .get<ApiResponse<Employee>>(`/employees/${publicId}`)
      .pipe(map((res) => res.data));
  }

  createEmployee(request: CreateEmployeeRequest): Observable<Employee> {
    return this.apiClient
      .post<ApiResponse<Employee>>('/employees', request)
      .pipe(map((res) => res.data));
  }

  updateEmployee(publicId: string, request: UpdateEmployeeRequest): Observable<Employee> {
    return this.apiClient
      .put<ApiResponse<Employee>>(`/employees/${publicId}`, request)
      .pipe(map((res) => res.data));
  }

  updateEmployeeStatus(publicId: string, request: UpdateEmployeeStatusRequest): Observable<Employee> {
    return this.apiClient
      .patch<ApiResponse<Employee>>(`/employees/${publicId}/status`, request)
      .pipe(map((res) => res.data));
  }

  getLifecycleHistory(publicId: string): Observable<EmployeeLifecycleEvent[]> {
    return this.apiClient
      .get<ApiResponse<EmployeeLifecycleEvent[]>>(`/employees/${publicId}/lifecycle-history`)
      .pipe(map((res) => res.data));
  }

  linkUserAccount(publicId: string, request: LinkUserAccountRequest): Observable<Employee> {
    return this.apiClient
      .post<ApiResponse<Employee>>(`/employees/${publicId}/user-account`, request)
      .pipe(map((res) => res.data));
  }

  unlinkUserAccount(publicId: string): Observable<Employee> {
    return this.apiClient
      .delete<ApiResponse<Employee>>(`/employees/${publicId}/user-account`)
      .pipe(map((res) => res.data));
  }

  uploadProfilePhoto(publicId: string, file: File): Observable<Employee> {
    const formData = new FormData();
    formData.append('file', file);
    return this.apiClient
      .post<ApiResponse<Employee>>(`/employees/${publicId}/photo`, formData)
      .pipe(map((res) => res.data));
  }

  getProfilePhotoBlob(publicId: string): Observable<Blob> {
    return this.apiClient.getBlob(`/employees/${publicId}/photo`);
  }

  deleteEmployee(publicId: string): Observable<void> {
    return this.apiClient
      .delete<ApiResponse<void>>(`/employees/${publicId}`)
      .pipe(map(() => void 0));
  }
}
