package com.careos.hms.hr.leave.repository;

import com.careos.hms.hr.leave.entity.LeaveApplication;
import com.careos.hms.hr.leave.entity.LeaveApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LeaveApplicationRepository extends JpaRepository<LeaveApplication, Long>, JpaSpecificationExecutor<LeaveApplication> {

    Optional<LeaveApplication> findByPublicIdAndHospitalIdAndDeletedFalse(String publicId, Long hospitalId);

    @Query("""
            SELECT la FROM LeaveApplication la
            JOIN FETCH la.leaveType
            JOIN FETCH la.employee e
            LEFT JOIN FETCH e.department
            WHERE la.publicId = :publicId
              AND la.hospitalId = :hospitalId
              AND la.deleted = false
            """)
    Optional<LeaveApplication> findWithDetailsByPublicIdAndHospitalId(
            @Param("publicId") String publicId,
            @Param("hospitalId") Long hospitalId);

    Page<LeaveApplication> findByHospitalIdAndEmployeeIdAndDeletedFalseOrderByAppliedAtDesc(
            Long hospitalId, Long employeeId, Pageable pageable);

    @Query("""
            SELECT la FROM LeaveApplication la
            WHERE la.hospitalId = :hospitalId
              AND la.employee.id = :employeeId
              AND la.status IN (:statuses)
              AND la.deleted = false
              AND (la.startDate <= :endDate AND la.endDate >= :startDate)
            """)
    List<LeaveApplication> findOverlappingActiveLeaves(
            @Param("hospitalId") Long hospitalId,
            @Param("employeeId") Long employeeId,
            @Param("statuses") List<LeaveApplicationStatus> statuses,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("""
            SELECT la FROM LeaveApplication la
            JOIN FETCH la.leaveType lt
            JOIN FETCH la.employee e
            LEFT JOIN FETCH e.department
            WHERE la.hospitalId = :hospitalId
              AND la.status IN (com.careos.hms.hr.leave.entity.LeaveApplicationStatus.APPROVED, com.careos.hms.hr.leave.entity.LeaveApplicationStatus.PENDING)
              AND la.deleted = false
              AND (la.startDate <= :endDate AND la.endDate >= :startDate)
            ORDER BY la.startDate ASC
            """)
    List<LeaveApplication> findCalendarLeaves(
            @Param("hospitalId") Long hospitalId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    long countByHospitalIdAndStatusAndDeletedFalse(Long hospitalId, LeaveApplicationStatus status);

    @Query("""
            SELECT COUNT(la) FROM LeaveApplication la
            WHERE la.hospitalId = :hospitalId
              AND la.status = com.careos.hms.hr.leave.entity.LeaveApplicationStatus.APPROVED
              AND la.deleted = false
              AND :date BETWEEN la.startDate AND la.endDate
            """)
    long countOnLeaveToday(@Param("hospitalId") Long hospitalId, @Param("date") LocalDate date);

    Page<LeaveApplication> findByHospitalIdAndDeletedFalseOrderByAppliedAtDesc(Long hospitalId, Pageable pageable);

    Page<LeaveApplication> findByHospitalIdAndStatusAndDeletedFalseOrderByAppliedAtDesc(Long hospitalId, LeaveApplicationStatus status, Pageable pageable);
}
