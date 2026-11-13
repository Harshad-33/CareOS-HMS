package com.careos.hms.hospital.repository;

import com.careos.hms.hospital.entity.HospitalContact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HospitalContactRepository extends JpaRepository<HospitalContact, Long> {

    List<HospitalContact> findByHospitalIdAndDeletedFalse(Long hospitalId);

    List<HospitalContact> findByHospitalPublicIdAndDeletedFalse(String hospitalPublicId);

    Optional<HospitalContact> findByPublicIdAndDeletedFalse(String publicId);

    Optional<HospitalContact> findByPublicIdAndHospitalIdAndDeletedFalse(String publicId, Long hospitalId);
}
