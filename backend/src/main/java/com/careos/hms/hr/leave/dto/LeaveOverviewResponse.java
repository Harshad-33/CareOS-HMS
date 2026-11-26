package com.careos.hms.hr.leave.dto;

import java.math.BigDecimal;
import java.util.List;

public record LeaveOverviewResponse(
        long activeLeaveTypesCount,
        long pendingApprovalsCount,
        long onLeaveTodayCount,
        BigDecimal myAvailableLeaveBalance,
        List<LeaveApplicationSummaryResponse> recentApplications,
        List<LeaveBalanceResponse> myBalances
) {
}
