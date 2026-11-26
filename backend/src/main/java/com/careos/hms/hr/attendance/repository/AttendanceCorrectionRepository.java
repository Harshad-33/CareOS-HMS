package com.careos.hms.hr.attendance.repository;

import com.careos.hms.hr.attendance.entity.AttendanceCorrection;
import com.careos.hms.hr.attendance.entity.CorrectionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AttendanceCorrectionRepository extends JpaRepository<AttendanceCorrection, Long> {

    Optional<AttendanceCorrection> findByPublicIdAndHospitalId(String publicId, Long hospitalId);

    @Query("""
            SELECT c FROM AttendanceCorrection c
            JOIN FETCH c.attendance a
            WHERE c.hospitalId = :hospitalId
              AND a.employee.id = :employeeId
            ORDER BY c.createdAt DESC
            """)
    Page<AttendanceCorrection> findMyCorrections(
            @Param("hospitalId") Long hospitalId,
            @Param("employeeId") Long employeeId,
            Pageable pageable);

    @Query("""
            SELECT c FROM AttendanceCorrection c
            JOIN FETCH c.attendance a
            JOIN FETCH a.employee e
            WHERE c.hospitalId = :hospitalId
              AND c.status = :status
            ORDER BY c.createdAt ASC
            """)
    Page<AttendanceCorrection> findPendingCorrections(
            @Param("hospitalId") Long hospitalId,
            @Param("status") CorrectionStatus status,
            Pageable pageable);
}
