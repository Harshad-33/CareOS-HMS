package com.careos.hms.hr.holiday.repository;

import com.careos.hms.hr.holiday.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HolidayRepository extends JpaRepository<Holiday, Long> {

    Optional<Holiday> findByPublicIdAndHospitalId(String publicId, Long hospitalId);

    @Query("""
            SELECT h FROM Holiday h
            LEFT JOIN FETCH h.branch
            WHERE h.hospitalId = :hospitalId
              AND h.holidayDate BETWEEN :startDate AND :endDate
              AND (h.branch IS NULL OR h.branch.id = :branchId)
            ORDER BY h.holidayDate ASC
            """)
    List<Holiday> findHolidaysForYearAndBranch(
            @Param("hospitalId") Long hospitalId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("branchId") Long branchId);

    boolean existsByHospitalIdAndHolidayDate(Long hospitalId, LocalDate holidayDate);
}
