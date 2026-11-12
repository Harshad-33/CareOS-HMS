package com.careos.hms.hospital.repository;

import com.careos.hms.hospital.entity.Hospital;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HospitalRepository extends JpaRepository<Hospital, Long> {

    Optional<Hospital> findByHospitalCodeAndDeletedFalse(String hospitalCode);

    Optional<Hospital> findByIdAndDeletedFalse(Long id);
}
