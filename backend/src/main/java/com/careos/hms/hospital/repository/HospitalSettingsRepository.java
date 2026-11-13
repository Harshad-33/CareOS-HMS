package com.careos.hms.hospital.repository;

import com.careos.hms.hospital.entity.HospitalSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HospitalSettingsRepository extends JpaRepository<HospitalSettings, Long> {

    Optional<HospitalSettings> findByHospitalId(Long hospitalId);

    Optional<HospitalSettings> findByHospitalPublicId(String publicId);
}
