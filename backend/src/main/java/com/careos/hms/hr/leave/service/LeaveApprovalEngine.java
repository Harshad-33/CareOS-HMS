package com.careos.hms.hr.leave.service;

import com.careos.hms.employee.entity.Employee;
import com.careos.hms.hr.leave.entity.ApproverRole;
import com.careos.hms.hr.leave.entity.LeaveApplication;
import com.careos.hms.hr.leave.entity.LeaveApproval;
import com.careos.hms.hr.leave.entity.LeaveApprovalStatus;
import com.careos.hms.hr.leave.entity.LeaveApprovalWorkflowType;
import com.careos.hms.hr.leave.entity.LeaveType;
import com.careos.hms.hr.leave.repository.LeaveApprovalRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class LeaveApprovalEngine {

    private final LeaveApprovalRepository leaveApprovalRepository;

    public LeaveApprovalEngine(LeaveApprovalRepository leaveApprovalRepository) {
        this.leaveApprovalRepository = leaveApprovalRepository;
    }

    public List<LeaveApproval> initializeApprovalSteps(
            Long hospitalId,
            LeaveApplication application,
            Employee employee,
            LeaveType leaveType
    ) {
        LeaveApprovalWorkflowType workflow = leaveType.getApprovalWorkflowType();
        List<LeaveApproval> steps = new ArrayList<>();

        switch (workflow) {
            case HR_ONLY -> {
                LeaveApproval step1 = new LeaveApproval(
                        hospitalId,
                        application,
                        1,
                        ApproverRole.HR_MANAGER,
                        null
                );
                steps.add(step1);
            }
            case MANAGER_ONLY -> {
                Employee manager = employee.getReportingManager();
                LeaveApproval step1 = new LeaveApproval(
                        hospitalId,
                        application,
                        1,
                        manager != null ? ApproverRole.REPORTING_MANAGER : ApproverRole.HR_MANAGER,
                        manager
                );
                steps.add(step1);
            }
            case MANAGER_THEN_HR -> {
                Employee manager = employee.getReportingManager();
                LeaveApproval step1 = new LeaveApproval(
                        hospitalId,
                        application,
                        1,
                        manager != null ? ApproverRole.REPORTING_MANAGER : ApproverRole.HR_MANAGER,
                        manager
                );
                steps.add(step1);

                LeaveApproval step2 = new LeaveApproval(
                        hospitalId,
                        application,
                        2,
                        ApproverRole.HR_MANAGER,
                        null
                );
                steps.add(step2);
            }
            case DEPT_HEAD_THEN_HR -> {
                Employee approver = employee.getReportingManager();
                LeaveApproval step1 = new LeaveApproval(
                        hospitalId,
                        application,
                        1,
                        ApproverRole.DEPARTMENT_HEAD,
                        approver
                );
                steps.add(step1);

                LeaveApproval step2 = new LeaveApproval(
                        hospitalId,
                        application,
                        2,
                        ApproverRole.HR_MANAGER,
                        null
                );
                steps.add(step2);
            }
        }

        return leaveApprovalRepository.saveAll(steps);
    }

    public boolean canUserApproveStep(
            LeaveApproval step,
            Long currentUserId,
            boolean isPlatformOrAdmin,
            boolean hasHrApprovalPermission
    ) {
        if (isPlatformOrAdmin) {
            return true;
        }

        if (step.getApproverRole() == ApproverRole.HR_MANAGER) {
            return hasHrApprovalPermission;
        }

        // For REPORTING_MANAGER or DEPARTMENT_HEAD:
        if (step.getAssignedApproverEmployee() != null && step.getAssignedApproverEmployee().getUser() != null) {
            if (step.getAssignedApproverEmployee().getUser().getId().equals(currentUserId)) {
                return true;
            }
        }

        // If no manager was assigned (fallback to HR), allow HR
        if (step.getAssignedApproverEmployee() == null && hasHrApprovalPermission) {
            return true;
        }

        return false;
    }
}
