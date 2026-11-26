package com.careos.hms.hr.leave.dto;

import com.careos.hms.hr.leave.entity.LeaveBalanceTransaction;
import com.careos.hms.hr.leave.entity.LeaveTransactionType;

import java.math.BigDecimal;
import java.time.Instant;

public record LeaveBalanceTransactionResponse(
        String publicId,
        String employeePublicId,
        String employeeNumber,
        String employeeName,
        String leaveTypeCode,
        String leaveTypeName,
        LeaveTransactionType transactionType,
        BigDecimal days,
        BigDecimal balanceBefore,
        BigDecimal balanceAfter,
        String referenceId,
        String remarks,
        String actorUsername,
        Instant createdAt
) {
    public static LeaveBalanceTransactionResponse from(LeaveBalanceTransaction tx) {
        String empName = tx.getEmployee() != null
                ? tx.getEmployee().getFirstName() + " " + tx.getEmployee().getLastName()
                : "";
        String empNum = tx.getEmployee() != null ? tx.getEmployee().getEmployeeNumber() : "";
        String empPubId = tx.getEmployee() != null ? tx.getEmployee().getPublicId() : "";

        String ltCode = tx.getLeaveType() != null ? tx.getLeaveType().getCode() : "";
        String ltName = tx.getLeaveType() != null ? tx.getLeaveType().getName() : "";

        return new LeaveBalanceTransactionResponse(
                tx.getPublicId(),
                empPubId,
                empNum,
                empName,
                ltCode,
                ltName,
                tx.getTransactionType(),
                tx.getDays(),
                tx.getBalanceBefore(),
                tx.getBalanceAfter(),
                tx.getReferenceId(),
                tx.getRemarks(),
                tx.getActorUsername(),
                tx.getCreatedAt()
        );
    }
}
