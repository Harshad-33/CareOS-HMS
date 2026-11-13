package com.careos.hms.department.entity;

import com.careos.hms.common.audit.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "departments")
public class Department extends TenantAwareEntity {

    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    private String publicId;

    @Column(name = "department_code", nullable = false, length = 32)
    private String departmentCode;

    @Column(name = "department_name", nullable = false, length = 100)
    private String departmentName;

    @Column(name = "description", length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private DepartmentStatus status = DepartmentStatus.ACTIVE;

    protected Department() {
    }

    public Department(Long hospitalId, String departmentCode, String departmentName, String description) {
        assignHospitalId(hospitalId);
        this.publicId = UUID.randomUUID().toString();
        this.departmentCode = departmentCode;
        this.departmentName = departmentName;
        this.description = description;
        this.status = DepartmentStatus.ACTIVE;
    }

    @PrePersist
    void ensurePublicId() {
        if (this.publicId == null || this.publicId.isBlank()) {
            this.publicId = UUID.randomUUID().toString();
        }
    }

    public void setHospitalId(Long hospitalId) {
        assignHospitalId(hospitalId);
    }

    public String getPublicId() {
        return publicId;
    }

    public String getDepartmentCode() {
        return departmentCode;
    }

    public void setDepartmentCode(String departmentCode) {
        this.departmentCode = departmentCode;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public DepartmentStatus getStatus() {
        return status;
    }

    public void setStatus(DepartmentStatus status) {
        this.status = status;
    }
}
