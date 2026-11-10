package com.careos.hms.common.audit;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class TenantAwareEntity extends SoftDeletableEntity {

    @Column(name = "hospital_id", nullable = false)
    private Long hospitalId;

    public Long getHospitalId() {
        return hospitalId;
    }

    protected void assignHospitalId(Long hospitalId) {
        this.hospitalId = hospitalId;
    }
}
