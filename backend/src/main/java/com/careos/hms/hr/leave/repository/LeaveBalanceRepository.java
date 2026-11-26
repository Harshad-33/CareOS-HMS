package com.careos.hms.hr.leave.repository;

import com.careos.hms.hr.leave.entity.LeaveBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {

    Optional<LeaveBalance> findByPublicIdAndHospitalId(String publicId, Long hospitalId);

    Optional<LeaveBalance> findByHospitalIdAndEmployeeIdAndLeaveTypeIdAndCalendarYear(
            Long hospitalId, Long employeeId, Long leaveTypeId, int calendarYear);

    @Query("""
            SELECT b FROM LeaveBalance b
            JOIN FETCH b.leaveType lt
            WHERE b.hospitalId = :hospitalId
              AND b.employee.id = :employeeId
              AND b.calendarYear = :calendarYear
              AND lt.deleted = false
            ORDER BY lt.name ASC
            """)
    List<LeaveBalance> findByHospitalIdAndEmployeeIdAndCalendarYearWithLeaveType(
            @Param("hospitalId") Long hospitalId,
            @Param("employeeId") Long employeeId,
            @Param("calendarYear") int calendarYear);
}
