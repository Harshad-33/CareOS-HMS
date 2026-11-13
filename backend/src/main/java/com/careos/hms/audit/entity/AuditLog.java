package com.careos.hms.audit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "actor_username", nullable = false, length = 64)
    private String actorUsername;

    @Column(name = "hospital_id")
    private Long hospitalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 64)
    private AuditAction action;

    @Column(name = "entity_name", nullable = false, length = 64)
    private String entityName;

    @Column(name = "entity_id", length = 64)
    private String entityId;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    @Column(name = "client_ip", length = 64)
    private String clientIp;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    protected AuditLog() {
    }

    public AuditLog(Long actorUserId, String actorUsername, Long hospitalId,
                    AuditAction action, String entityName, String entityId,
                    String description, String metadataJson, String clientIp) {
        this.actorUserId = actorUserId;
        this.actorUsername = actorUsername;
        this.hospitalId = hospitalId;
        this.action = action;
        this.entityName = entityName;
        this.entityId = entityId;
        this.description = description;
        this.metadataJson = metadataJson;
        this.clientIp = clientIp;
        this.timestamp = Instant.now();
    }

    @PrePersist
    void ensureTimestamp() {
        if (this.timestamp == null) {
            this.timestamp = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Long getActorUserId() {
        return actorUserId;
    }

    public String getActorUsername() {
        return actorUsername;
    }

    public Long getHospitalId() {
        return hospitalId;
    }

    public AuditAction getAction() {
        return action;
    }

    public String getEntityName() {
        return entityName;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getDescription() {
        return description;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public String getClientIp() {
        return clientIp;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
