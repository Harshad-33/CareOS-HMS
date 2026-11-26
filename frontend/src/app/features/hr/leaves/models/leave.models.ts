export type LeaveApprovalWorkflowType =
  | 'HR_ONLY'
  | 'MANAGER_ONLY'
  | 'MANAGER_THEN_HR'
  | 'DEPT_HEAD_THEN_HR';

export type LeaveApplicationStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED';

export type LeaveApprovalStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED';

export type HalfDayPeriod = 'FIRST_HALF' | 'SECOND_HALF';

export type LeaveTransactionType =
  | 'ALLOCATION'
  | 'MANUAL_ADJUSTMENT'
  | 'DEBIT_APPROVED_LEAVE'
  | 'CREDIT_REVERSED_LEAVE'
  | 'EXPIRY'
  | 'ENCASHMENT'
  | 'REVERSAL';

export interface LeaveType {
  publicId: string;
  code: string;
  name: string;
  description?: string;
  annualAllowance: number;
  carryForwardAllowed: boolean;
  maxCarryForwardDays: number;
  requiresDocument: boolean;
  maxConsecutiveDays: number;
  allowHalfDay: boolean;
  approvalWorkflow: LeaveApprovalWorkflowType;
  approvalLevelsCount: number;
  status: 'ACTIVE' | 'INACTIVE';
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateLeaveTypeRequest {
  code: string;
  name: string;
  description?: string;
  annualAllowance: number;
  carryForwardAllowed: boolean;
  maxCarryForwardDays: number;
  requiresDocument: boolean;
  maxConsecutiveDays: number;
  allowHalfDay: boolean;
  approvalWorkflow: LeaveApprovalWorkflowType;
  approvalLevelsCount: number;
}

export interface UpdateLeaveTypeRequest {
  name: string;
  description?: string;
  annualAllowance: number;
  carryForwardAllowed: boolean;
  maxCarryForwardDays: number;
  requiresDocument: boolean;
  maxConsecutiveDays: number;
  allowHalfDay: boolean;
  approvalWorkflow: LeaveApprovalWorkflowType;
  approvalLevelsCount: number;
}

export interface LeaveBalance {
  publicId: string;
  employeePublicId: string;
  employeeName: string;
  employeeNumber: string;
  departmentName?: string;
  branchName?: string;
  leaveTypePublicId: string;
  leaveTypeCode: string;
  leaveTypeName: string;
  year: number;
  totalAllocated: number;
  carriedForward: number;
  usedDays: number;
  pendingApprovalDays: number;
  availableBalance: number;
  version?: number;
}

export interface AllocateLeaveBalanceRequest {
  employeePublicId: string;
  leaveTypePublicId: string;
  year: number;
  allocatedDays: number;
  reason?: string;
}

export interface AdjustLeaveBalanceRequest {
  adjustmentDays: number;
  reason: string;
}

export interface LeaveBalanceTransaction {
  publicId: string;
  leaveTypePublicId: string;
  leaveTypeName: string;
  transactionType: LeaveTransactionType;
  days: number;
  balanceAfter: number;
  reason: string;
  referenceApplicationPublicId?: string;
  performedBy: string;
  transactionTimestamp: string;
}

export interface LeaveApprovalStep {
  stepNumber: number;
  stepName: string;
  approverRoleOrUserId?: string;
  approverName?: string;
  status: LeaveApprovalStatus;
  actionTakenByUsername?: string;
  comments?: string;
  decisionTimestamp?: string;
}

export interface LeaveApplication {
  publicId: string;
  applicationNumber: string;
  employeePublicId: string;
  employeeName: string;
  employeeNumber: string;
  departmentName?: string;
  leaveTypePublicId: string;
  leaveTypeCode: string;
  leaveTypeName: string;
  startDate: string;
  endDate: string;
  halfDay: boolean;
  halfDayPeriod?: HalfDayPeriod;
  totalDays: number;
  reason: string;
  emergencyContactPhone?: string;
  documentKey?: string;
  hasDocument: boolean;
  documentOriginalFilename?: string;
  status: LeaveApplicationStatus;
  appliedAt: string;
  currentStepNumber: number;
  requiredStepsCount: number;
  finalDecisionAt?: string;
  finalRejectionReason?: string;
  approvalSteps: LeaveApprovalStep[];
}

export interface ApplyLeaveRequest {
  employeePublicId?: string;
  leaveTypePublicId: string;
  startDate: string;
  endDate: string;
  halfDay: boolean;
  halfDayPeriod?: HalfDayPeriod;
  reason: string;
  emergencyContactPhone?: string;
  documentKey?: string;
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
  employeeName: string;
  departmentName?: string;
  leaveTypeName: string;
  startDate: string;
  endDate: string;
  halfDay: boolean;
  halfDayPeriod?: HalfDayPeriod;
  totalDays: number;
  status: LeaveApplicationStatus;
}

export interface LeaveOverview {
  totalApplications: number;
  pendingApprovals: number;
  approvedThisMonth: number;
  employeesOnLeaveToday: number;
  totalLeaveTypes: number;
  recentApplications: LeaveApplication[];
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface LeaveFilterParams {
  page?: number;
  size?: number;
  employeePublicId?: string;
  departmentPublicId?: string;
  leaveTypePublicId?: string;
  status?: LeaveApplicationStatus;
  year?: number;
  startDate?: string;
  endDate?: string;
  search?: string;
}
