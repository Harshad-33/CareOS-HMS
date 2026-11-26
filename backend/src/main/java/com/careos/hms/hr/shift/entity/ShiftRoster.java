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

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "shift_rosters")
public class ShiftRoster extends BaseEntity {

    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    private String publicId;

    @Column(name = "hospital_id", nullable = false)
    private Long hospitalId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shift_id", nullable = false)
    private Shift shift;

    @Column(name = "roster_date", nullable = false)
    private LocalDate rosterDate;

    @Column(name = "notes", length = 255)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private RosterStatus status = RosterStatus.SCHEDULED;

    protected ShiftRoster() {
    }

    public ShiftRoster(Long hospitalId, Employee employee, Shift shift, LocalDate rosterDate) {
        this.publicId = UUID.randomUUID().toString();
        this.hospitalId = hospitalId;
        this.employee = employee;
        this.shift = shift;
        this.rosterDate = rosterDate;
        this.status = RosterStatus.SCHEDULED;
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

    public Employee getEmployee() {
        return employee;
    }

    public Shift getShift() {
        return shift;
    }

    public void setShift(Shift shift) {
        this.shift = shift;
    }

    public LocalDate getRosterDate() {
        return rosterDate;
    }

    public void setRosterDate(LocalDate rosterDate) {
        this.rosterDate = rosterDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public RosterStatus getStatus() {
        return status;
    }

    public void setStatus(RosterStatus status) {
        this.status = status;
    }
}
