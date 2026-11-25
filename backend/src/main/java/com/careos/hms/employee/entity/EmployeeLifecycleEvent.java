package com.careos.hms.employee.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "employee_lifecycle_events")
public class EmployeeLifecycleEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    private String publicId;

    @Column(name = "hospital_id", nullable = false)
    private Long hospitalId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 24)
    private EmployeeStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 24)
    private EmployeeStatus newStatus;

    @Column(name = "reason", nullable = false, length = 255)
    private String reason;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "actor_username", nullable = false, length = 64)
    private String actorUsername;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected EmployeeLifecycleEvent() {
    }

    public EmployeeLifecycleEvent(Long hospitalId, Employee employee, EmployeeStatus previousStatus,
                                  EmployeeStatus newStatus, String reason, LocalDate effectiveDate,
                                  String actorUsername) {
        this.publicId = UUID.randomUUID().toString();
        this.hospitalId = hospitalId;
        this.employee = employee;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.reason = reason;
        this.effectiveDate = effectiveDate;
        this.actorUsername = actorUsername;
        this.createdAt = Instant.now();
    }

    @PrePersist
    void ensurePublicId() {
        if (this.publicId == null || this.publicId.isBlank()) {
            this.publicId = UUID.randomUUID().toString();
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getPublicId() {
        return publicId;
    }

    public Long getHospitalId() {
        return hospitalId;
    }

    public Employee getEmployee() {
        return employee;
    }

    public EmployeeStatus getPreviousStatus() {
        return previousStatus;
    }

    public EmployeeStatus getNewStatus() {
        return newStatus;
    }

    public String getReason() {
        return reason;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public String getActorUsername() {
        return actorUsername;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
