package com.careos.hms.hospital.repository;

import com.careos.hms.hospital.entity.HospitalDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HospitalDocumentRepository extends JpaRepository<HospitalDocument, Long> {

    List<HospitalDocument> findByHospitalIdAndDeletedFalse(Long hospitalId);

    List<HospitalDocument> findByHospitalPublicIdAndDeletedFalse(String hospitalPublicId);

    Optional<HospitalDocument> findByPublicIdAndDeletedFalse(String publicId);

    Optional<HospitalDocument> findByPublicIdAndHospitalIdAndDeletedFalse(String publicId, Long hospitalId);
}
