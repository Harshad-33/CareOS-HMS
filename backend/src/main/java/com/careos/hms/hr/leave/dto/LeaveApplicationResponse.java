package com.careos.hms.hr.leave.dto;

import com.careos.hms.hr.leave.entity.HalfDayPeriod;
import com.careos.hms.hr.leave.entity.LeaveApplication;
import com.careos.hms.hr.leave.entity.LeaveApplicationStatus;
import com.careos.hms.hr.leave.entity.LeaveApproval;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

public record LeaveApplicationResponse(
        String publicId,
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
        LeaveApplicationStatus status,
        int currentStepNumber,
        int requiredStepsCount,
        Instant appliedAt,
        Instant finalDecisionAt,
        String finalRejectionReason,
        List<LeaveApprovalStepResponse> approvals,
        Instant createdAt,
        Instant updatedAt
) {
    public static LeaveApplicationResponse from(LeaveApplication app, List<LeaveApproval> approvals) {
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

        List<LeaveApprovalStepResponse> stepResponses = approvals != null
                ? approvals.stream().map(LeaveApprovalStepResponse::from).toList()
                : Collections.emptyList();

        return new LeaveApplicationResponse(
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
                app.getStatus(),
                app.getCurrentStepNumber(),
                app.getRequiredStepsCount(),
                app.getAppliedAt(),
                app.getFinalDecisionAt(),
                app.getFinalRejectionReason(),
                stepResponses,
                app.getCreatedAt(),
                app.getUpdatedAt()
        );
    }
}
