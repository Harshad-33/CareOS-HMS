package com.careos.hms.hr.shift.repository;

import com.careos.hms.hr.shift.entity.Shift;
import com.careos.hms.hr.shift.entity.ShiftStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface ShiftRepository extends JpaRepository<Shift, Long>, JpaSpecificationExecutor<Shift> {

    Optional<Shift> findByPublicIdAndHospitalIdAndDeletedFalse(String publicId, Long hospitalId);

    List<Shift> findByHospitalIdAndDeletedFalseOrderByStartTimeAsc(Long hospitalId);

    List<Shift> findByHospitalIdAndStatusAndDeletedFalseOrderByStartTimeAsc(Long hospitalId, ShiftStatus status);

    boolean existsByHospitalIdAndCodeIgnoreCaseAndDeletedFalse(Long hospitalId, String code);
}
