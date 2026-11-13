import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { ApiResponse } from '../../../core/models/api-response.model';
import { ApiClientService } from '../../../core/services/api-client.service';
import {
  AuditLog,
  Department,
  DepartmentRequest,
  Hospital,
  HospitalBranch,
  HospitalBranchRequest,
  HospitalContact,
  HospitalContactRequest,
  HospitalDocument,
  HospitalRegistrationRequest,
  HospitalSettings,
  HospitalStatus,
  UpdateHospitalProfileRequest,
  UpdateHospitalStatusRequest,
  VerifyDocumentRequest,
} from '../models/hospital.model';

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

@Injectable({
  providedIn: 'root',
})
export class HospitalService {
  private readonly apiClient = inject(ApiClientService);
  private readonly http = inject(HttpClient);

  // --- Hospital Management ---
  registerHospital(request: HospitalRegistrationRequest): Observable<Hospital> {
    return this.apiClient
      .post<ApiResponse<Hospital>>('/hospitals', request)
      .pipe(map((res) => res.data));
  }

  getHospitals(
    status?: HospitalStatus,
    search?: string,
    page = 0,
    size = 20
  ): Observable<PageResponse<Hospital>> {
    const params: Record<string, string | number | boolean | null | undefined> = {
      page,
      size,
    };
    if (status) params['status'] = status;
    if (search) params['search'] = search;

    return this.apiClient
      .get<ApiResponse<PageResponse<Hospital>>>('/hospitals', { params })
      .pipe(map((res) => res.data));
  }

  getMyHospital(): Observable<Hospital> {
    return this.apiClient
      .get<ApiResponse<Hospital>>('/hospitals/me')
      .pipe(map((res) => res.data));
  }

  getHospitalByPublicId(publicId: string): Observable<Hospital> {
    return this.apiClient
      .get<ApiResponse<Hospital>>(`/hospitals/${publicId}`)
      .pipe(map((res) => res.data));
  }

  updateMyHospital(request: UpdateHospitalProfileRequest): Observable<Hospital> {
    return this.apiClient
      .put<ApiResponse<Hospital>>('/hospitals/me', request)
      .pipe(map((res) => res.data));
  }

  updateHospitalProfile(publicId: string, request: UpdateHospitalProfileRequest): Observable<Hospital> {
    return this.apiClient
      .put<ApiResponse<Hospital>>(`/hospitals/${publicId}`, request)
      .pipe(map((res) => res.data));
  }

  updateHospitalStatus(publicId: string, request: UpdateHospitalStatusRequest): Observable<Hospital> {
    return this.apiClient
      .patch<ApiResponse<Hospital>>(`/hospitals/${publicId}/status`, request)
      .pipe(map((res) => res.data));
  }

  // --- Settings ---
  getMySettings(): Observable<HospitalSettings> {
    return this.apiClient
      .get<ApiResponse<HospitalSettings>>('/hospitals/me/settings')
      .pipe(map((res) => res.data));
  }

  updateMySettings(settings: Partial<HospitalSettings>): Observable<HospitalSettings> {
    return this.apiClient
      .put<ApiResponse<HospitalSettings>>('/hospitals/me/settings', settings)
      .pipe(map((res) => res.data));
  }

  getHospitalSettingsByPublicId(publicId: string): Observable<HospitalSettings> {
    return this.apiClient
      .get<ApiResponse<HospitalSettings>>(`/hospitals/${publicId}/settings`)
      .pipe(map((res) => res.data));
  }

  // --- Contacts ---
  getMyContacts(): Observable<HospitalContact[]> {
    return this.apiClient
      .get<ApiResponse<HospitalContact[]>>('/hospitals/me/contacts')
      .pipe(map((res) => res.data));
  }

  createMyContact(request: HospitalContactRequest): Observable<HospitalContact> {
    return this.apiClient
      .post<ApiResponse<HospitalContact>>('/hospitals/me/contacts', request)
      .pipe(map((res) => res.data));
  }

  updateMyContact(contactPublicId: string, request: HospitalContactRequest): Observable<HospitalContact> {
    return this.apiClient
      .put<ApiResponse<HospitalContact>>(`/hospitals/me/contacts/${contactPublicId}`, request)
      .pipe(map((res) => res.data));
  }

  deleteMyContact(contactPublicId: string): Observable<void> {
    return this.apiClient
      .delete<ApiResponse<void>>(`/hospitals/me/contacts/${contactPublicId}`)
      .pipe(map(() => undefined));
  }

  // --- Documents ---
  getMyDocuments(): Observable<HospitalDocument[]> {
    return this.apiClient
      .get<ApiResponse<HospitalDocument[]>>('/hospitals/me/documents')
      .pipe(map((res) => res.data));
  }

  uploadMyDocument(file: File, documentType: string, expiryDate?: string): Observable<HospitalDocument> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('documentType', documentType);
    if (expiryDate) {
      formData.append('expiryDate', expiryDate);
    }
    return this.apiClient
      .post<ApiResponse<HospitalDocument>>('/hospitals/me/documents', formData)
      .pipe(map((res) => res.data));
  }

  downloadDocument(publicId: string): Observable<Blob> {
    return this.apiClient.getBlob(`/documents/${publicId}/download`);
  }

  verifyDocument(publicId: string, request: VerifyDocumentRequest): Observable<HospitalDocument> {
    return this.apiClient
      .patch<ApiResponse<HospitalDocument>>(`/documents/${publicId}/verify`, request)
      .pipe(map((res) => res.data));
  }

  deleteMyDocument(publicId: string): Observable<void> {
    return this.apiClient
      .delete<ApiResponse<void>>(`/hospitals/me/documents/${publicId}`)
      .pipe(map(() => undefined));
  }

  getHospitalDocuments(publicId: string): Observable<HospitalDocument[]> {
    return this.apiClient
      .get<ApiResponse<HospitalDocument[]>>(`/hospitals/${publicId}/documents`)
      .pipe(map((res) => res.data));
  }

  // --- Departments ---
  getMyDepartments(): Observable<Department[]> {
    return this.apiClient
      .get<ApiResponse<Department[]>>('/departments')
      .pipe(map((res) => res.data));
  }

  createMyDepartment(request: DepartmentRequest): Observable<Department> {
    return this.apiClient
      .post<ApiResponse<Department>>('/departments', request)
      .pipe(map((res) => res.data));
  }

  updateMyDepartment(publicId: string, request: DepartmentRequest): Observable<Department> {
    return this.apiClient
      .put<ApiResponse<Department>>(`/departments/${publicId}`, request)
      .pipe(map((res) => res.data));
  }

  toggleDepartmentStatus(publicId: string): Observable<Department> {
    return this.apiClient
      .patch<ApiResponse<Department>>(`/departments/${publicId}/status`, {})
      .pipe(map((res) => res.data));
  }

  deleteMyDepartment(publicId: string): Observable<void> {
    return this.apiClient
      .delete<ApiResponse<void>>(`/departments/${publicId}`)
      .pipe(map(() => undefined));
  }

  // --- Branches ---
  getMyBranches(): Observable<HospitalBranch[]> {
    return this.apiClient
      .get<ApiResponse<HospitalBranch[]>>('/branches')
      .pipe(map((res) => res.data));
  }

  createMyBranch(request: HospitalBranchRequest): Observable<HospitalBranch> {
    return this.apiClient
      .post<ApiResponse<HospitalBranch>>('/branches', request)
      .pipe(map((res) => res.data));
  }

  updateMyBranch(publicId: string, request: HospitalBranchRequest): Observable<HospitalBranch> {
    return this.apiClient
      .put<ApiResponse<HospitalBranch>>(`/branches/${publicId}`, request)
      .pipe(map((res) => res.data));
  }

  toggleBranchStatus(publicId: string): Observable<HospitalBranch> {
    return this.apiClient
      .patch<ApiResponse<HospitalBranch>>(`/branches/${publicId}/status`, {})
      .pipe(map((res) => res.data));
  }

  deleteMyBranch(publicId: string): Observable<void> {
    return this.apiClient
      .delete<ApiResponse<void>>(`/branches/${publicId}`)
      .pipe(map(() => undefined));
  }

  // --- Audits ---
  getGlobalAudits(page = 0, size = 20): Observable<PageResponse<AuditLog>> {
    return this.apiClient
      .get<ApiResponse<PageResponse<AuditLog>>>('/audits', { params: { page, size } })
      .pipe(map((res) => res.data));
  }

  getMyHospitalAudits(page = 0, size = 20): Observable<PageResponse<AuditLog>> {
    return this.apiClient
      .get<ApiResponse<PageResponse<AuditLog>>>('/audits/hospital', { params: { page, size } })
      .pipe(map((res) => res.data));
  }
}