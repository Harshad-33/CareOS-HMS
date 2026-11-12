package com.careos.hms.hospital.entity;

import com.careos.hms.common.audit.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Minimal hospital tenant anchor entity introduced in Phase 2 to provide database
 * foreign-key integrity for hospital-scoped users and tenant-aware authorization.
 * Full hospital registration and administration belong to Phase 3.
 */
@Entity
@Table(name = "hospitals")
public class Hospital extends SoftDeletableEntity {

    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    private String publicId;

    @Column(name = "hospital_code", nullable = false, unique = true, length = 32)
    private String hospitalCode;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private HospitalStatus status = HospitalStatus.ACTIVE;

    protected Hospital() {
    }

    public Hospital(String hospitalCode, String name) {
        this.publicId = UUID.randomUUID().toString();
        this.hospitalCode = hospitalCode;
        this.name = name;
        this.status = HospitalStatus.ACTIVE;
    }

    @PrePersist
    void ensurePublicId() {
        if (this.publicId == null || this.publicId.isBlank()) {
            this.publicId = UUID.randomUUID().toString();
        }
    }

    public String getPublicId() {
        return publicId;
    }

    public String getHospitalCode() {
        return hospitalCode;
    }

    public String getName() {
        return name;
    }

    public HospitalStatus getStatus() {
        return status;
    }

    public void setStatus(HospitalStatus status) {
        this.status = status;
    }
}
