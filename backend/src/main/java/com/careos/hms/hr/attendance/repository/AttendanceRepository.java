package com.careos.hms.hr.attendance.repository;

import com.careos.hms.hr.attendance.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long>, JpaSpecificationExecutor<Attendance> {

    Optional<Attendance> findByPublicIdAndHospitalId(String publicId, Long hospitalId);

    Optional<Attendance> findByHospitalIdAndEmployeeIdAndAttendanceDate(Long hospitalId, Long employeeId, LocalDate attendanceDate);

    @Query("""
            SELECT a FROM Attendance a
            LEFT JOIN FETCH a.shift
            WHERE a.hospitalId = :hospitalId
              AND a.employee.id = :employeeId
              AND a.attendanceDate BETWEEN :startDate AND :endDate
            ORDER BY a.attendanceDate ASC
            """)
    List<Attendance> findMonthlyAttendanceForEmployee(
            @Param("hospitalId") Long hospitalId,
            @Param("employeeId") Long employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("""
            SELECT a FROM Attendance a
            JOIN FETCH a.employee e
            LEFT JOIN FETCH a.shift
            WHERE a.hospitalId = :hospitalId
              AND a.attendanceDate = :date
              AND (:departmentId IS NULL OR e.department.id = :departmentId)
            ORDER BY e.firstName ASC
            """)
    List<Attendance> findDailyRegister(
            @Param("hospitalId") Long hospitalId,
            @Param("date") LocalDate date,
            @Param("departmentId") Long departmentId);
}
