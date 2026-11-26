package com.careos.hms.hr.shift.repository;

import com.careos.hms.hr.shift.entity.ShiftRoster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ShiftRosterRepository extends JpaRepository<ShiftRoster, Long> {

    Optional<ShiftRoster> findByPublicIdAndHospitalId(String publicId, Long hospitalId);

    Optional<ShiftRoster> findByHospitalIdAndEmployeeIdAndRosterDate(Long hospitalId, Long employeeId, LocalDate rosterDate);

    @Query("""
            SELECT r FROM ShiftRoster r
            JOIN FETCH r.shift
            WHERE r.hospitalId = :hospitalId
              AND r.employee.id = :employeeId
              AND r.rosterDate BETWEEN :startDate AND :endDate
            ORDER BY r.rosterDate ASC
            """)
    List<ShiftRoster> findByHospitalIdAndEmployeeIdAndRosterDateBetween(
            @Param("hospitalId") Long hospitalId,
            @Param("employeeId") Long employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("""
            SELECT r FROM ShiftRoster r
            JOIN FETCH r.shift
            JOIN FETCH r.employee e
            WHERE r.hospitalId = :hospitalId
              AND e.department.id = :departmentId
              AND r.rosterDate BETWEEN :startDate AND :endDate
            ORDER BY r.rosterDate ASC, e.firstName ASC
            """)
    List<ShiftRoster> findByHospitalIdAndDepartmentIdAndRosterDateBetween(
            @Param("hospitalId") Long hospitalId,
            @Param("departmentId") Long departmentId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
