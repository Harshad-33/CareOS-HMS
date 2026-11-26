package com.careos.hms.hr.leave.dto;

import com.careos.hms.hr.leave.entity.LeaveApplication;
import com.careos.hms.hr.leave.entity.LeaveApplicationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LeaveCalendarItemResponse(
        String applicationPublicId,
        String employeePublicId,
        String employeeNumber,
        String employeeName,
        String departmentName,
        String leaveTypeCode,
        String leaveTypeName,
        boolean paid,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal totalDays,
        boolean halfDay,
        LeaveApplicationStatus status
) {
    public static LeaveCalendarItemResponse from(LeaveApplication app) {
        String empPubId = app.getEmployee() != null ? app.getEmployee().getPublicId() : "";
        String empNum = app.getEmployee() != null ? app.getEmployee().getEmployeeNumber() : "";
        String empName = app.getEmployee() != null
                ? app.getEmployee().getFirstName() + " " + app.getEmployee().getLastName()
                : "";
        String deptName = (app.getEmployee() != null && app.getEmployee().getDepartment() != null)
                ? app.getEmployee().getDepartment().getDepartmentName()
                : "";

        String ltCode = app.getLeaveType() != null ? app.getLeaveType().getCode() : "";
        String ltName = app.getLeaveType() != null ? app.getLeaveType().getName() : "";
        boolean isPaid = app.getLeaveType() != null && app.getLeaveType().isPaid();

        return new LeaveCalendarItemResponse(
                app.getPublicId(),
                empPubId,
                empNum,
                empName,
                deptName,
                ltCode,
                ltName,
                isPaid,
                app.getStartDate(),
                app.getEndDate(),
                app.getTotalDays(),
                app.isHalfDay(),
                app.getStatus()
        );
    }
}
