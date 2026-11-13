package com.careos.hms.hospital.repository;

import com.careos.hms.hospital.entity.Hospital;
import com.careos.hms.hospital.entity.HospitalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface HospitalRepository extends JpaRepository<Hospital, Long> {

    Optional<Hospital> findByHospitalCodeAndDeletedFalse(String hospitalCode);

    Optional<Hospital> findByIdAndDeletedFalse(Long id);

    Optional<Hospital> findByPublicIdAndDeletedFalse(String publicId);

    boolean existsByHospitalCodeAndDeletedFalse(String hospitalCode);

    boolean existsByRegistrationNumberAndDeletedFalse(String registrationNumber);

    Page<Hospital> findAllByDeletedFalse(Pageable pageable);

    Page<Hospital> findByStatusAndDeletedFalse(HospitalStatus status, Pageable pageable);

    @Query("""
        SELECT h FROM Hospital h
        WHERE h.deleted = false
          AND (:status IS NULL OR h.status = :status)
          AND (:search IS NULL OR LOWER(h.name) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(h.hospitalCode) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(h.city) LIKE LOWER(CONCAT('%', :search, '%')))
    """)
    Page<Hospital> searchHospitals(@Param("status") HospitalStatus status,
                                   @Param("search") String search,
                                   Pageable pageable);
}
