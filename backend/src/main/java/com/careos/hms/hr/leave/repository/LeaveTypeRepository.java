package com.careos.hms.hr.leave.repository;

import com.careos.hms.hr.leave.entity.LeaveType;
import com.careos.hms.hr.leave.entity.LeaveTypeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface LeaveTypeRepository extends JpaRepository<LeaveType, Long>, JpaSpecificationExecutor<LeaveType> {

    Optional<LeaveType> findByPublicIdAndHospitalIdAndDeletedFalse(String publicId, Long hospitalId);

    List<LeaveType> findByHospitalIdAndDeletedFalseOrderByCodeAsc(Long hospitalId);

    List<LeaveType> findByHospitalIdAndStatusAndDeletedFalseOrderByCodeAsc(Long hospitalId, LeaveTypeStatus status);

    boolean existsByHospitalIdAndCodeIgnoreCaseAndDeletedFalse(Long hospitalId, String code);
}
