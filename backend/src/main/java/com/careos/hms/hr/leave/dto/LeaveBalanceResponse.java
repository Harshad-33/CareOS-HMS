package com.careos.hms.hr.leave.dto;

import com.careos.hms.hr.leave.entity.LeaveBalance;

import java.math.BigDecimal;
import java.time.Instant;

public record LeaveBalanceResponse(
        String publicId,
        String employeePublicId,
        String employeeNumber,
        String employeeName,
        String leaveTypePublicId,
        String leaveTypeCode,
        String leaveTypeName,
        int calendarYear,
        BigDecimal totalAllocated,
        BigDecimal usedDays,
        BigDecimal pendingApprovalDays,
        BigDecimal availableBalance,
        Long version,
        Instant updatedAt
) {
    public static LeaveBalanceResponse from(LeaveBalance balance) {
        String empName = balance.getEmployee() != null
                ? balance.getEmployee().getFirstName() + " " + balance.getEmployee().getLastName()
                : "";
        String empNum = balance.getEmployee() != null ? balance.getEmployee().getEmployeeNumber() : "";
        String empPubId = balance.getEmployee() != null ? balance.getEmployee().getPublicId() : "";

        String ltPubId = balance.getLeaveType() != null ? balance.getLeaveType().getPublicId() : "";
        String ltCode = balance.getLeaveType() != null ? balance.getLeaveType().getCode() : "";
        String ltName = balance.getLeaveType() != null ? balance.getLeaveType().getName() : "";

        return new LeaveBalanceResponse(
                balance.getPublicId(),
                empPubId,
                empNum,
                empName,
                ltPubId,
                ltCode,
                ltName,
                balance.getCalendarYear(),
                balance.getTotalAllocated(),
                balance.getUsedDays(),
                balance.getPendingApprovalDays(),
                balance.getAvailableBalance(),
                balance.getVersion(),
                balance.getUpdatedAt()
        );
    }
}
