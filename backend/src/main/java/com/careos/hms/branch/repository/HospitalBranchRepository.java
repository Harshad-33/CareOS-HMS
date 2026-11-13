package com.careos.hms.branch.repository;

import com.careos.hms.branch.entity.HospitalBranch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HospitalBranchRepository extends JpaRepository<HospitalBranch, Long> {

    List<HospitalBranch> findByHospitalIdAndDeletedFalseOrderByBranchNameAsc(Long hospitalId);

    Optional<HospitalBranch> findByPublicIdAndDeletedFalse(String publicId);

    Optional<HospitalBranch> findByPublicIdAndHospitalIdAndDeletedFalse(String publicId, Long hospitalId);

    boolean existsByHospitalIdAndBranchCodeIgnoreCaseAndDeletedFalse(Long hospitalId, String branchCode);

    boolean existsByHospitalIdAndBranchCodeIgnoreCaseAndIdNotAndDeletedFalse(Long hospitalId, String branchCode, Long id);
}
