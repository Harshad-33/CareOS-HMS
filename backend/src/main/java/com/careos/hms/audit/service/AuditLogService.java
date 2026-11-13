package com.careos.hms.audit.service;

import com.careos.hms.audit.dto.AuditLogResponse;
import com.careos.hms.audit.entity.AuditAction;
import com.careos.hms.audit.entity.AuditLog;
import com.careos.hms.audit.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void record(Long actorUserId, String actorUsername, Long hospitalId,
                       AuditAction action, String entityName, String entityId,
                       String description, String metadataJson, String clientIp) {
        AuditLog auditLog = new AuditLog(
                actorUserId,
                actorUsername != null ? actorUsername : "SYSTEM",
                hospitalId,
                action,
                entityName,
                entityId,
                description,
                metadataJson,
                clientIp
        );
        auditLogRepository.save(auditLog);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getHospitalAuditLogs(Long hospitalId, Pageable pageable) {
        return auditLogRepository.findByHospitalIdOrderByTimestampDesc(hospitalId, pageable)
                .map(AuditLogResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAllAuditLogs(Pageable pageable) {
        return auditLogRepository.findAllByOrderByTimestampDesc(pageable)
                .map(AuditLogResponse::from);
    }
}
