package com.careos.hms.hr.leave.repository;

import com.careos.hms.hr.leave.entity.LeaveBalanceTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LeaveBalanceTransactionRepository extends JpaRepository<LeaveBalanceTransaction, Long> {

    @Query("""
            SELECT t FROM LeaveBalanceTransaction t
            JOIN FETCH t.leaveType
            WHERE t.hospitalId = :hospitalId
              AND t.employee.id = :employeeId
            ORDER BY t.createdAt DESC
            """)
    Page<LeaveBalanceTransaction> findByHospitalIdAndEmployeeIdOrderByCreatedAtDesc(
            @Param("hospitalId") Long hospitalId,
            @Param("employeeId") Long employeeId,
            Pageable pageable);
}
