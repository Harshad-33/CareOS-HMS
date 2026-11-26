package com.careos.hms.hr.document.repository;

import com.careos.hms.hr.document.entity.HrDocument;
import com.careos.hms.hr.document.entity.HrDocumentCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HrDocumentRepository extends JpaRepository<HrDocument, Long>, JpaSpecificationExecutor<HrDocument> {

    Optional<HrDocument> findByPublicIdAndHospitalIdAndDeletedFalse(String publicId, Long hospitalId);

    List<HrDocument> findByHospitalIdAndEmployeeIdAndDeletedFalseOrderByIssueDateDesc(Long hospitalId, Long employeeId);

    List<HrDocument> findByHospitalIdAndEmployeeIdAndCategoryAndDeletedFalse(
            Long hospitalId, Long employeeId, HrDocumentCategory category);

    @Query("""
            SELECT d FROM HrDocument d
            JOIN FETCH d.employee e
            WHERE d.hospitalId = :hospitalId
              AND d.deleted = false
              AND d.expiryDate IS NOT NULL
              AND d.expiryDate BETWEEN :today AND :cutoffDate
            ORDER BY d.expiryDate ASC
            """)
    Page<HrDocument> findExpiringDocuments(
            @Param("hospitalId") Long hospitalId,
            @Param("today") LocalDate today,
            @Param("cutoffDate") LocalDate cutoffDate,
            Pageable pageable);
}
