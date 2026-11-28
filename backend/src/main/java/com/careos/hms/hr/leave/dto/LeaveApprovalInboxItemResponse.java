package com.careos.hms.hr.leave.dto;

import com.careos.hms.hr.leave.entity.ApproverRole;
import com.careos.hms.hr.leave.entity.HalfDayPeriod;
import com.careos.hms.hr.leave.entity.LeaveApplication;
import com.careos.hms.hr.leave.entity.LeaveApproval;
import com.careos.hms.hr.leave.entity.LeaveApprovalStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record LeaveApprovalInboxItemResponse(
        String approvalStepPublicId,
        String applicationPublicId,
        String employeePublicId,
        String employeeNumber,
        String employeeName,
        String departmentName,
        String leaveTypePublicId,
        String leaveTypeCode,
        String leaveTypeName,
        boolean paid,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal totalDays,
        boolean halfDay,
        HalfDayPeriod halfDayPeriod,
        String reason,
        String contactPhoneDuringLeave,
        String documentStorageKey,
        int stepNumber,
        int requiredStepsCount,
        ApproverRole approverRole,
        String assignedApproverEmployeePublicId,
        String assignedApproverName,
        LeaveApprovalStatus stepStatus,
        Instant appliedAt
) {
    public static LeaveApprovalInboxItemResponse from(LeaveApproval approval) {
        LeaveApplication app = approval.getLeaveApplication();
        String empPubId = app.getEmployee() != null ? app.getEmployee().getPublicId() : "";
        String empNum = app.getEmployee() != null ? app.getEmployee().getEmployeeNumber() : "";
        String empName = app.getEmployee() != null
                ? app.getEmployee().getFirstName() + " " + app.getEmployee().getLastName()
                : "";
        String deptName = (app.getEmployee() != null && app.getEmployee().getDepartment() != null)
                ? app.getEmployee().getDepartment().getDepartmentName()
                : "";

        String ltPubId = app.getLeaveType() != null ? app.getLeaveType().getPublicId() : "";
        String ltCode = app.getLeaveType() != null ? app.getLeaveType().getCode() : "";
        String ltName = app.getLeaveType() != null ? app.getLeaveType().getName() : "";
        boolean isPaid = app.getLeaveType() != null && app.getLeaveType().isPaid();

        String approverPubId = approval.getAssignedApproverEmployee() != null
                ? approval.getAssignedApproverEmployee().getPublicId()
                : null;
        String approverName = approval.getAssignedApproverEmployee() != null
                ? approval.getAssignedApproverEmployee().getFirstName() + " " + approval.getAssignedApproverEmployee().getLastName()
                : (approval.getApproverRole() == ApproverRole.HR_MANAGER ? "HR Department" : null);

        return new LeaveApprovalInboxItemResponse(
                approval.getPublicId(),
                app.getPublicId(),
                empPubId,
                empNum,
                empName,
                deptName,
                ltPubId,
                ltCode,
                ltName,
                isPaid,
                app.getStartDate(),
                app.getEndDate(),
                app.getTotalDays(),
                app.isHalfDay(),
                app.getHalfDayPeriod(),
                app.getReason(),
                app.getContactPhoneDuringLeave(),
                app.getDocumentStorageKey(),
                approval.getStepNumber(),
                app.getRequiredStepsCount(),
                approval.getApproverRole(),
                approverPubId,
                approverName,
                approval.getStatus(),
                app.getAppliedAt()
        );
    }
}
