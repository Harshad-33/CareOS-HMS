package com.careos.hms.hr.leave.repository;

import com.careos.hms.hr.leave.entity.LeaveApproval;
import com.careos.hms.hr.leave.entity.LeaveApprovalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LeaveApprovalRepository extends JpaRepository<LeaveApproval, Long> {

    List<LeaveApproval> findByLeaveApplicationIdOrderByStepNumberAsc(Long leaveApplicationId);

    Optional<LeaveApproval> findByLeaveApplicationIdAndStepNumber(Long leaveApplicationId, int stepNumber);

    Optional<LeaveApproval> findByPublicIdAndHospitalId(String publicId, Long hospitalId);

    @Query("""
            SELECT la FROM LeaveApproval la
            JOIN FETCH la.leaveApplication app
            JOIN FETCH app.employee emp
            JOIN FETCH app.leaveType lt
            LEFT JOIN FETCH emp.department
            WHERE la.hospitalId = :hospitalId
              AND la.status = :status
              AND app.status = com.careos.hms.hr.leave.entity.LeaveApplicationStatus.PENDING
              AND app.currentStepNumber = la.stepNumber
              AND (
                  (la.assignedApproverEmployee.id = :approverEmployeeId)
                  OR (:isHr = true AND (la.approverRole = com.careos.hms.hr.leave.entity.ApproverRole.HR_MANAGER OR la.assignedApproverEmployee IS NULL))
              )
            ORDER BY app.appliedAt ASC
            """)
    Page<LeaveApproval> findPendingApprovalsForUser(
            @Param("hospitalId") Long hospitalId,
            @Param("status") LeaveApprovalStatus status,
            @Param("approverEmployeeId") Long approverEmployeeId,
            @Param("isHr") boolean isHr,
            Pageable pageable);

    @Query("""
            SELECT COUNT(la) FROM LeaveApproval la
            JOIN la.leaveApplication app
            WHERE la.hospitalId = :hospitalId
              AND la.status = com.careos.hms.hr.leave.entity.LeaveApprovalStatus.PENDING
              AND app.status = com.careos.hms.hr.leave.entity.LeaveApplicationStatus.PENDING
              AND app.currentStepNumber = la.stepNumber
              AND (
                  (la.assignedApproverEmployee.id = :approverEmployeeId)
                  OR (:isHr = true AND (la.approverRole = com.careos.hms.hr.leave.entity.ApproverRole.HR_MANAGER OR la.assignedApproverEmployee IS NULL))
              )
            """)
    long countPendingApprovalsForUser(
            @Param("hospitalId") Long hospitalId,
            @Param("approverEmployeeId") Long approverEmployeeId,
            @Param("isHr") boolean isHr);
}
