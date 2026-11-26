package com.careos.hms.hr.shift.entity;

import com.careos.hms.common.audit.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "shifts")
public class Shift extends TenantAwareEntity {

    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    private String publicId;

    @Column(name = "code", nullable = false, length = 32)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "grace_period_minutes", nullable = false)
    private int gracePeriodMinutes = 15;

    @Column(name = "break_duration_minutes", nullable = false)
    private int breakDurationMinutes = 30;

    @Column(name = "is_cross_midnight", nullable = false)
    private boolean crossMidnight = false;

    @Column(name = "color_code", nullable = false, length = 16)
    private String colorCode = "#1976D2";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private ShiftStatus status = ShiftStatus.ACTIVE;

    protected Shift() {
    }

    public Shift(Long hospitalId, String code, String name, LocalTime startTime, LocalTime endTime) {
        assignHospitalId(hospitalId);
        this.publicId = UUID.randomUUID().toString();
        this.code = code;
        this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;
        this.crossMidnight = endTime.isBefore(startTime);
        this.status = ShiftStatus.ACTIVE;
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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
        if (this.endTime != null) {
            this.crossMidnight = this.endTime.isBefore(startTime);
        }
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
        if (this.startTime != null) {
            this.crossMidnight = endTime.isBefore(this.startTime);
        }
    }

    public int getGracePeriodMinutes() {
        return gracePeriodMinutes;
    }

    public void setGracePeriodMinutes(int gracePeriodMinutes) {
        this.gracePeriodMinutes = gracePeriodMinutes;
    }

    public int getBreakDurationMinutes() {
        return breakDurationMinutes;
    }

    public void setBreakDurationMinutes(int breakDurationMinutes) {
        this.breakDurationMinutes = breakDurationMinutes;
    }

    public boolean isCrossMidnight() {
        return crossMidnight;
    }

    public void setCrossMidnight(boolean crossMidnight) {
        this.crossMidnight = crossMidnight;
    }

    public String getColorCode() {
        return colorCode;
    }

    public void setColorCode(String colorCode) {
        this.colorCode = colorCode;
    }

    public ShiftStatus getStatus() {
        return status;
    }

    public void setStatus(ShiftStatus status) {
        this.status = status;
    }
}
