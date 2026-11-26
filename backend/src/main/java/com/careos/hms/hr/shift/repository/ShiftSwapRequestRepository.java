package com.careos.hms.hr.shift.repository;

import com.careos.hms.hr.shift.entity.ShiftSwapRequest;
import com.careos.hms.hr.shift.entity.ShiftSwapStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ShiftSwapRequestRepository extends JpaRepository<ShiftSwapRequest, Long> {

    Optional<ShiftSwapRequest> findByPublicIdAndHospitalId(String publicId, Long hospitalId);

    @Query("""
            SELECT s FROM ShiftSwapRequest s
            JOIN FETCH s.requestingEmployee
            JOIN FETCH s.targetEmployee
            JOIN FETCH s.sourceRoster sr JOIN FETCH sr.shift
            JOIN FETCH s.targetRoster tr JOIN FETCH tr.shift
            WHERE s.hospitalId = :hospitalId
              AND (s.requestingEmployee.id = :employeeId OR s.targetEmployee.id = :employeeId)
            ORDER BY s.createdAt DESC
            """)
    Page<ShiftSwapRequest> findMySwaps(
            @Param("hospitalId") Long hospitalId,
            @Param("employeeId") Long employeeId,
            Pageable pageable);

    @Query("""
            SELECT s FROM ShiftSwapRequest s
            JOIN FETCH s.requestingEmployee
            JOIN FETCH s.targetEmployee
            JOIN FETCH s.sourceRoster sr JOIN FETCH sr.shift
            JOIN FETCH s.targetRoster tr JOIN FETCH tr.shift
            WHERE s.hospitalId = :hospitalId
              AND s.status = :status
            ORDER BY s.createdAt ASC
            """)
    Page<ShiftSwapRequest> findPendingManagerApprovals(
            @Param("hospitalId") Long hospitalId,
            @Param("status") ShiftSwapStatus status,
            Pageable pageable);
}
