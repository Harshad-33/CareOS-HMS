package com.careos.hms.audit.repository;

import com.careos.hms.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByHospitalIdOrderByTimestampDesc(Long hospitalId, Pageable pageable);

    Page<AuditLog> findAllByOrderByTimestampDesc(Pageable pageable);
}
