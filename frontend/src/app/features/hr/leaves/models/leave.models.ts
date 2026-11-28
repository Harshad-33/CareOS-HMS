export type LeaveApprovalWorkflowType =
  | 'HR_ONLY'
  | 'MANAGER_ONLY'
  | 'MANAGER_THEN_HR'
  | 'DEPT_HEAD_THEN_HR';

export type ApproverRole = 'REPORTING_MANAGER' | 'DEPARTMENT_HEAD' | 'HR_MANAGER';

export type LeaveApplicationStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED';

export type LeaveApprovalStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED';

export type HalfDayPeriod = 'FIRST_HALF' | 'SECOND_HALF';

export type LeaveTransactionType =
  | 'ALLOCATION'
  | 'MANUAL_ADJUSTMENT'
  | 'DEBIT_APPROVED_LEAVE'
  | 'CREDIT_REVERSED_LEAVE';

export interface LeaveType {
  publicId: string;
  code: string;
  name: string;
  description?: string | null;
  annualAllowance: number;
  carryForwardEnabled: boolean;
  maxCarryForwardDays: number;
  medicalCertRequired: boolean;
  minServiceDaysRequired: number;
  paid: boolean;
  approvalWorkflowType: LeaveApprovalWorkflowType;
  approvalLevelsCount: number;
  status: 'ACTIVE' | 'INACTIVE';
  createdAt?: string;
  updatedAt?: string;
}

export interface LeaveTypeSummary {
  publicId: string;
  code: string;
  name: string;
  annualAllowance: number;
  paid: boolean;
  medicalCertRequired: boolean;
  approvalWorkflowType: LeaveApprovalWorkflowType;
  status: 'ACTIVE' | 'INACTIVE';
}

export interface CreateLeaveTypeRequest {
  code: string;
  name: string;
  description?: string;
  annualAllowance: number;
  carryForwardEnabled: boolean;
  maxCarryForwardDays: number;
  medicalCertRequired: boolean;
  minServiceDaysRequired: number;
  paid: boolean;
  approvalWorkflowType: LeaveApprovalWorkflowType;
  approvalLevelsCount: number;
}

export interface UpdateLeaveTypeRequest {
  name: string;
  description?: string;
  annualAllowance: number;
  carryForwardEnabled: boolean;
  maxCarryForwardDays: number;
  medicalCertRequired: boolean;
  minServiceDaysRequired: number;
  paid: boolean;
  approvalWorkflowType: LeaveApprovalWorkflowType;
  approvalLevelsCount: number;
}

export interface LeaveBalance {
  publicId: string;
  employeePublicId: string;
  employeeNumber: string;
  employeeName: string;
  leaveTypePublicId: string;
  leaveTypeCode: string;
  leaveTypeName: string;
  calendarYear: number;
  totalAllocated: number;
  usedDays: number;
  pendingApprovalDays: number;
  availableBalance: number;
  version?: number;
  updatedAt?: string;
}

export interface AllocateLeaveBalanceRequest {
  employeePublicId: string;
  leaveTypePublicId: string;
  calendarYear: number;
  allocatedDays: number;
  remarks?: string;
}

export interface AdjustLeaveBalanceRequest {
  adjustmentDays: number;
  reason: string;
}

export interface LeaveBalanceTransaction {
  publicId: string;
  employeePublicId: string;
  employeeNumber: string;
  employeeName: string;
  leaveTypeCode: string;
  leaveTypeName: string;
  transactionType: LeaveTransactionType;
  days: number;
  balanceBefore: number;
  balanceAfter: number;
  referenceId?: string | null;
  remarks?: string | null;
  actorUsername: string;
  createdAt: string;
}

export interface LeaveApprovalStep {
  publicId: string;
  stepNumber: number;
  approverRole: ApproverRole;
  assignedApproverEmployeePublicId?: string | null;
  assignedApproverName?: string | null;
  actionTakenByUsername?: string | null;
  status: LeaveApprovalStatus;
  decisionTimestamp?: string | null;
  comments?: string | null;
  rejectionReason?: string | null;
}

export interface LeaveApprovalInboxItem {
  approvalStepPublicId: string;
  applicationPublicId: string;
  employeePublicId: string;
  employeeNumber: string;
  employeeName: string;
  departmentName?: string | null;
  leaveTypePublicId: string;
  leaveTypeCode: string;
  leaveTypeName: string;
  paid: boolean;
  startDate: string;
  endDate: string;
  totalDays: number;
  halfDay: boolean;
  halfDayPeriod?: HalfDayPeriod | null;
  reason: string;
  contactPhoneDuringLeave?: string | null;
  documentStorageKey?: string | null;
  stepNumber: number;
  requiredStepsCount: number;
  approverRole: ApproverRole;
  assignedApproverEmployeePublicId?: string | null;
  assignedApproverName?: string | null;
  stepStatus: LeaveApprovalStatus;
  appliedAt: string;
}

export interface LeaveApplication {
  publicId: string;
  employeePublicId: string;
  employeeNumber: string;
  employeeName: string;
  departmentName?: string | null;
  leaveTypePublicId: string;
  leaveTypeCode: string;
  leaveTypeName: string;
  paid: boolean;
  startDate: string;
  endDate: string;
  totalDays: number;
  halfDay: boolean;
  halfDayPeriod?: HalfDayPeriod | null;
  reason: string;
  contactPhoneDuringLeave?: string | null;
  documentStorageKey?: string | null;
  status: LeaveApplicationStatus;
  currentStepNumber: number;
  requiredStepsCount: number;
  appliedAt: string;
  finalDecisionAt?: string | null;
  finalRejectionReason?: string | null;
  approvals: LeaveApprovalStep[];
  createdAt?: string;
  updatedAt?: string;
}

export interface LeaveApplicationSummary {
  publicId: string;
  employeePublicId: string;
  employeeNumber: string;
  employeeName: string;
  departmentName?: string | null;
  leaveTypeCode: string;
  leaveTypeName: string;
  paid: boolean;
  startDate: string;
  endDate: string;
  totalDays: number;
  halfDay: boolean;
  status: LeaveApplicationStatus;
  currentStepNumber: number;
  requiredStepsCount: number;
  appliedAt: string;
}

export interface ApplyLeaveRequest {
  employeePublicId?: string;
  leaveTypePublicId: string;
  startDate: string;
  endDate: string;
  halfDay: boolean;
  halfDayPeriod?: HalfDayPeriod;
  reason: string;
  contactPhoneDuringLeave?: string;
  documentStorageKey?: string;
}

export interface ApproveLeaveStepRequest {
  comments?: string;
}

export interface RejectLeaveStepRequest {
  rejectionReason: string;
  comments?: string;
}

export interface LeaveCalendarItem {
  applicationPublicId: string;
  employeePublicId: string;
  employeeNumber: string;
  employeeName: string;
  departmentName?: string | null;
  leaveTypeCode: string;
  leaveTypeName: string;
  paid: boolean;
  startDate: string;
  endDate: string;
  totalDays: number;
  halfDay: boolean;
  status: LeaveApplicationStatus;
}

export interface LeaveOverview {
  activeLeaveTypesCount: number;
  pendingApprovalsCount: number;
  onLeaveTodayCount: number;
  myAvailableLeaveBalance: number;
  recentApplications: LeaveApplicationSummary[];
  myBalances: LeaveBalance[];
}

export interface UploadDocumentResponse {
  documentStorageKey: string;
  originalFilename: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first?: boolean;
  last?: boolean;
  empty?: boolean;
}

export interface LeaveFilterParams {
  page?: number;
  size?: number;
  status?: LeaveApplicationStatus;
}
