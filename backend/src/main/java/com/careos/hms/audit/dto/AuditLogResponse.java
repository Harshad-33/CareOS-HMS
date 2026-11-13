package com.careos.hms.audit.dto;

import com.careos.hms.audit.entity.AuditAction;
import com.careos.hms.audit.entity.AuditLog;

import java.time.Instant;

public record AuditLogResponse(
        Long id,
        Long actorUserId,
        String actorUsername,
        Long hospitalId,
        AuditAction action,
        String entityName,
        String entityId,
        String description,
        String metadataJson,
        String clientIp,
        Instant timestamp
) {
    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getActorUserId(),
                log.getActorUsername(),
                log.getHospitalId(),
                log.getAction(),
                log.getEntityName(),
                log.getEntityId(),
                log.getDescription(),
                log.getMetadataJson(),
                log.getClientIp(),
                log.getTimestamp()
        );
    }
}
