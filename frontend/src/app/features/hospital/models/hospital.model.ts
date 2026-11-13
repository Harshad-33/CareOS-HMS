export type HospitalStatus = 'PENDING' | 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';

export type HospitalType =
  | 'GENERAL'
  | 'MULTI_SPECIALTY'
  | 'SUPER_SPECIALTY'
  | 'CLINIC'
  | 'DIAGNOSTIC_CENTER';

export interface Hospital {
  id: number;
  publicId: string;
  hospitalCode: string;
  registrationNumber?: string;
  name: string;
  hospitalType: HospitalType;
  email?: string;
  primaryContactNumber?: string;
  alternateContactNumber?: string;
  website?: string;
  addressLine1?: string;
  addressLine2?: string;
  city?: string;
  district?: string;
  state?: string;
  country?: string;
  pinCode?: string;
  establishedDate?: string;
  status: HospitalStatus;
  createdAt: string;
  updatedAt: string;
}

export interface HospitalRegistrationRequest {
  hospitalName: string;
  hospitalCode?: string;
  registrationNumber: string;
  hospitalType: HospitalType;
  email: string;
  primaryContactNumber: string;
  alternateContactNumber?: string;
  website?: string;
  addressLine1: string;
  addressLine2?: string;
  city: string;
  district: string;
  state: string;
  country?: string;
  pinCode: string;
  establishedDate?: string;
  adminUsername: string;
  adminEmail: string;
  adminPassword: string;
  adminFirstName: string;
  adminLastName: string;
}

export interface UpdateHospitalProfileRequest {
  name: string;
  hospitalType: HospitalType;
  email: string;
  primaryContactNumber: string;
  alternateContactNumber?: string;
  website?: string;
  addressLine1: string;
  addressLine2?: string;
  city: string;
  district: string;
  state: string;
  country?: string;
  pinCode: string;
  establishedDate?: string;
}

export interface UpdateHospitalStatusRequest {
  status: HospitalStatus;
  reason?: string;
}

export interface HospitalSettings {
  id: number;
  timezone: string;
  currency: string;
  dateFormat: string;
  timeFormat: string;
  workingDays: string;
  workingHoursStart: string;
  workingHoursEnd: string;
  appointmentSlotDurationMinutes: number;
  maxAdvanceBookingDays: number;
  emergencyContactNumber?: string;
  supportEmail?: string;
  updatedAt: string;
}

export type ContactType = 'GENERAL' | 'EMERGENCY' | 'BILLING' | 'ADMINISTRATIVE' | 'TECHNICAL';

export interface HospitalContact {
  publicId: string;
  contactType: ContactType;
  name: string;
  designation?: string;
  phoneNumber: string;
  email?: string;
  primary: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface HospitalContactRequest {
  contactType: ContactType;
  name: string;
  designation?: string;
  phoneNumber: string;
  email?: string;
  primary: boolean;
}

export type DocumentVerificationStatus = 'PENDING' | 'VERIFIED' | 'REJECTED';

export interface HospitalDocument {
  publicId: string;
  documentType: string;
  documentName: string;
  fileKey: string;
  contentType: string;
  fileSizeBytes: number;
  uploadDate: string;
  expiryDate?: string;
  verificationStatus: DocumentVerificationStatus;
  verifiedAt?: string;
  verifiedBy?: string;
  rejectionReason?: string;
}

export interface VerifyDocumentRequest {
  verificationStatus: DocumentVerificationStatus;
  rejectionReason?: string;
}

export type DepartmentStatus = 'ACTIVE' | 'INACTIVE';

export interface Department {
  id: number;
  publicId: string;
  departmentCode: string;
  departmentName: string;
  description?: string;
  status: DepartmentStatus;
  createdAt: string;
  updatedAt: string;
}

export interface DepartmentRequest {
  departmentCode: string;
  departmentName: string;
  description?: string;
  status?: DepartmentStatus;
}

export type BranchStatus = 'ACTIVE' | 'INACTIVE';

export interface HospitalBranch {
  id: number;
  publicId: string;
  branchCode: string;
  branchName: string;
  contactNumber?: string;
  email?: string;
  address?: string;
  city?: string;
  district?: string;
  state?: string;
  country?: string;
  pinCode?: string;
  status: BranchStatus;
  createdAt: string;
  updatedAt: string;
}

export interface HospitalBranchRequest {
  branchCode: string;
  branchName: string;
  contactNumber?: string;
  email?: string;
  address?: string;
  city?: string;
  district?: string;
  state?: string;
  country?: string;
  pinCode?: string;
  status?: BranchStatus;
}

export interface AuditLog {
  id: number;
  actorUserId?: number;
  actorUsername: string;
  hospitalId?: number;
  action: string;
  entityName: string;
  entityId?: string;
  description?: string;
  metadataJson?: string;
  clientIp?: string;
  timestamp: string;
}