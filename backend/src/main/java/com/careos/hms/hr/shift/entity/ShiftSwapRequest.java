package com.careos.hms.hr.shift.entity;

import com.careos.hms.common.audit.BaseEntity;
import com.careos.hms.employee.entity.Employee;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "shift_swap_requests")
public class ShiftSwapRequest extends BaseEntity {

    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    private String publicId;

    @Column(name = "hospital_id", nullable = false)
    private Long hospitalId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requesting_employee_id", nullable = false)
    private Employee requestingEmployee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_employee_id", nullable = false)
    private Employee targetEmployee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_roster_id", nullable = false)
    private ShiftRoster sourceRoster;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_roster_id", nullable = false)
    private ShiftRoster targetRoster;

    @Column(name = "reason", nullable = false, length = 255)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private ShiftSwapStatus status = ShiftSwapStatus.PENDING_PEER;

    @Column(name = "rejection_reason", length = 255)
    private String rejectionReason;

    protected ShiftSwapRequest() {
    }

    public ShiftSwapRequest(Long hospitalId, Employee requestingEmployee, Employee targetEmployee,
                            ShiftRoster sourceRoster, ShiftRoster targetRoster, String reason) {
        this.publicId = UUID.randomUUID().toString();
        this.hospitalId = hospitalId;
        this.requestingEmployee = requestingEmployee;
        this.targetEmployee = targetEmployee;
        this.sourceRoster = sourceRoster;
        this.targetRoster = targetRoster;
        this.reason = reason;
        this.status = ShiftSwapStatus.PENDING_PEER;
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

    public Long getHospitalId() {
        return hospitalId;
    }

    public Employee getRequestingEmployee() {
        return requestingEmployee;
    }

    public Employee getTargetEmployee() {
        return targetEmployee;
    }

    public ShiftRoster getSourceRoster() {
        return sourceRoster;
    }

    public ShiftRoster getTargetRoster() {
        return targetRoster;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public ShiftSwapStatus getStatus() {
        return status;
    }

    public void setStatus(ShiftSwapStatus status) {
        this.status = status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}
