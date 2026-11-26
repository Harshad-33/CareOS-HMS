package com.careos.hms.hr.attendance.entity;

import com.careos.hms.common.audit.BaseEntity;
import com.careos.hms.employee.entity.Employee;
import com.careos.hms.hr.shift.entity.Shift;
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
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "attendances")
public class Attendance extends BaseEntity {

    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    private String publicId;

    @Column(name = "hospital_id", nullable = false)
    private Long hospitalId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id")
    private Shift shift;

    @Column(name = "check_in_time")
    private LocalTime checkInTime;

    @Column(name = "check_out_time")
    private LocalTime checkOutTime;

    @Column(name = "total_work_minutes", nullable = false)
    private int totalWorkMinutes = 0;

    @Column(name = "break_minutes", nullable = false)
    private int breakMinutes = 0;

    @Column(name = "overtime_minutes", nullable = false)
    private int overtimeMinutes = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private AttendanceStatus status = AttendanceStatus.PRESENT;

    @Enumerated(EnumType.STRING)
    @Column(name = "ingest_source", nullable = false, length = 32)
    private IngestSource ingestSource = IngestSource.WEB;

    @Column(name = "device_id", length = 64)
    private String deviceId;

    @Column(name = "biometric_log_id", length = 64)
    private String biometricLogId;

    @Column(name = "remarks", length = 255)
    private String remarks;

    protected Attendance() {
    }

    public Attendance(Long hospitalId, Employee employee, LocalDate attendanceDate, AttendanceStatus status) {
        this.publicId = UUID.randomUUID().toString();
        this.hospitalId = hospitalId;
        this.employee = employee;
        this.attendanceDate = attendanceDate;
        this.status = status;
        this.ingestSource = IngestSource.WEB;
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

    public LocalDate getAttendanceDate() {
        return attendanceDate;
    }

    public Shift getShift() {
        return shift;
    }

    public void setShift(Shift shift) {
        this.shift = shift;
    }

    public LocalTime getCheckInTime() {
        return checkInTime;
    }

    public void setCheckInTime(LocalTime checkInTime) {
        this.checkInTime = checkInTime;
    }

    public LocalTime getCheckOutTime() {
        return checkOutTime;
    }

    public void setCheckOutTime(LocalTime checkOutTime) {
        this.checkOutTime = checkOutTime;
    }

    public int getTotalWorkMinutes() {
        return totalWorkMinutes;
    }

    public void setTotalWorkMinutes(int totalWorkMinutes) {
        this.totalWorkMinutes = totalWorkMinutes;
    }

    public int getBreakMinutes() {
        return breakMinutes;
    }

    public void setBreakMinutes(int breakMinutes) {
        this.breakMinutes = breakMinutes;
    }

    public int getOvertimeMinutes() {
        return overtimeMinutes;
    }

    public void setOvertimeMinutes(int overtimeMinutes) {
        this.overtimeMinutes = overtimeMinutes;
    }

    public AttendanceStatus getStatus() {
        return status;
    }

    public void setStatus(AttendanceStatus status) {
        this.status = status;
    }

    public IngestSource getIngestSource() {
        return ingestSource;
    }

    public void setIngestSource(IngestSource ingestSource) {
        this.ingestSource = ingestSource;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getBiometricLogId() {
        return biometricLogId;
    }

    public void setBiometricLogId(String biometricLogId) {
        this.biometricLogId = biometricLogId;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
