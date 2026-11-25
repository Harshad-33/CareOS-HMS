export type EmployeeGender = 'MALE' | 'FEMALE' | 'OTHER';

export type EmploymentType =
  | 'FULL_TIME'
  | 'PART_TIME'
  | 'CONTRACT'
  | 'PROBATION'
  | 'INTERN'
  | 'VISITING_CONSULTANT';

export type EmployeeStatus =
  | 'ACTIVE'
  | 'INACTIVE'
  | 'SUSPENDED'
  | 'RESIGNED'
  | 'TERMINATED';

export type AccountStatus = 'ACTIVE' | 'INACTIVE' | 'LOCKED' | 'SUSPENDED';

export interface LinkedUserAccount {
  publicId: string;
  username: string;
  email: string;
  status: AccountStatus;
  roleCodes: string[];
}

export interface EmployeeSummary {
  publicId: string;
  employeeNumber: string;
  firstName: string;
  middleName?: string;
  lastName: string;
  fullName: string;
  gender: EmployeeGender;
  designation: string;
  departmentPublicId?: string;
  departmentName?: string;
  branchPublicId?: string;
  branchName?: string;
  employmentType: EmploymentType;
  status: EmployeeStatus;
  email: string;
  phoneNumber: string;
  joiningDate: string;
  profilePhotoKey?: string;
  hasUserAccount: boolean;
  userPublicId?: string;
  username?: string;
}

export interface Employee {
  publicId: string;
  employeeNumber: string;
  firstName: string;
  middleName?: string;
  lastName: string;
  fullName: string;
  dateOfBirth: string;
  gender: EmployeeGender;
  bloodGroup?: string;
  email: string;
  phoneNumber: string;
  alternatePhoneNumber?: string;
  addressLine1?: string;
  addressLine2?: string;
  city?: string;
  district?: string;
  state?: string;
  country?: string;
  pinCode?: string;
  emergencyContactName?: string;
  emergencyContactRelation?: string;
  emergencyContactPhone?: string;
  designation: string;
  departmentPublicId?: string;
  departmentName?: string;
  branchPublicId?: string;
  branchName?: string;
  employmentType: EmploymentType;
  status: EmployeeStatus;
  joiningDate: string;
  leavingDate?: string;
  reportingManagerPublicId?: string;
  reportingManagerName?: string;
  reportingManagerDesignation?: string;
  profilePhotoKey?: string;
  hasUserAccount: boolean;
  userAccount?: LinkedUserAccount;
  createdAt: string;
  updatedAt: string;
}

export interface CreateEmployeeRequest {
  firstName: string;
  middleName?: string;
  lastName: string;
  dateOfBirth: string;
  gender: EmployeeGender;
  bloodGroup?: string;
  email: string;
  phoneNumber: string;
  alternatePhoneNumber?: string;
  addressLine1?: string;
  addressLine2?: string;
  city?: string;
  district?: string;
  state?: string;
  country?: string;
  pinCode?: string;
  emergencyContactName?: string;
  emergencyContactRelation?: string;
  emergencyContactPhone?: string;
  designation: string;
  departmentPublicId: string;
  branchPublicId?: string;
  employmentType: EmploymentType;
  joiningDate: string;
  employeeNumber?: string;
  reportingManagerPublicId?: string;
  createUserAccount?: boolean;
  username?: string;
  password?: string;
  roleCodes?: string[];
}

export interface UpdateEmployeeRequest {
  firstName: string;
  middleName?: string;
  lastName: string;
  dateOfBirth: string;
  gender: EmployeeGender;
  bloodGroup?: string;
  email: string;
  phoneNumber: string;
  alternatePhoneNumber?: string;
  addressLine1?: string;
  addressLine2?: string;
  city?: string;
  district?: string;
  state?: string;
  country?: string;
  pinCode?: string;
  emergencyContactName?: string;
  emergencyContactRelation?: string;
  emergencyContactPhone?: string;
  designation: string;
  departmentPublicId: string;
  branchPublicId?: string;
  employmentType: EmploymentType;
  joiningDate: string;
  leavingDate?: string;
  reportingManagerPublicId?: string;
}

export interface UpdateEmployeeStatusRequest {
  status: EmployeeStatus;
  reason: string;
  effectiveDate?: string;
}

export interface LinkUserAccountRequest {
  userPublicId?: string;
  username?: string;
  password?: string;
  roleCodes?: string[];
}

export interface EmployeeLifecycleEvent {
  publicId: string;
  previousStatus?: EmployeeStatus;
  newStatus: EmployeeStatus;
  reason: string;
  effectiveDate: string;
  actorUsername: string;
  createdAt: string;
}

export interface EmployeeFilterParams {
  search?: string;
  departmentPublicId?: string;
  branchPublicId?: string;
  designation?: string;
  employmentType?: EmploymentType;
  status?: EmployeeStatus;
  page?: number;
  size?: number;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
