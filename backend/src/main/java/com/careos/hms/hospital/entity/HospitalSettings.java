package com.careos.hms.hospital.entity;

import com.careos.hms.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalTime;

@Entity
@Table(name = "hospital_settings")
public class HospitalSettings extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id", nullable = false, unique = true)
    private Hospital hospital;

    @Column(name = "timezone", nullable = false, length = 64)
    private String timezone = "Asia/Kolkata";

    @Column(name = "currency", nullable = false, length = 10)
    private String currency = "INR";

    @Column(name = "date_format", nullable = false, length = 32)
    private String dateFormat = "YYYY-MM-DD";

    @Column(name = "time_format", nullable = false, length = 16)
    private String timeFormat = "24_HOUR";

    @Column(name = "working_days", nullable = false, length = 100)
    private String workingDays = "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY";

    @Column(name = "working_hours_start", nullable = false)
    private LocalTime workingHoursStart = LocalTime.of(9, 0);

    @Column(name = "working_hours_end", nullable = false)
    private LocalTime workingHoursEnd = LocalTime.of(18, 0);

    @Column(name = "appointment_slot_duration_minutes", nullable = false)
    private Integer appointmentSlotDurationMinutes = 15;

    @Column(name = "max_advance_booking_days", nullable = false)
    private Integer maxAdvanceBookingDays = 30;

    @Column(name = "emergency_contact_number", length = 32)
    private String emergencyContactNumber;

    @Column(name = "support_email", length = 160)
    private String supportEmail;

    protected HospitalSettings() {
    }

    public HospitalSettings(Hospital hospital) {
        this.hospital = hospital;
    }

    public Hospital getHospital() {
        return hospital;
    }

    public void setHospital(Hospital hospital) {
        this.hospital = hospital;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    public String getTimeFormat() {
        return timeFormat;
    }

    public void setTimeFormat(String timeFormat) {
        this.timeFormat = timeFormat;
    }

    public String getWorkingDays() {
        return workingDays;
    }

    public void setWorkingDays(String workingDays) {
        this.workingDays = workingDays;
    }

    public LocalTime getWorkingHoursStart() {
        return workingHoursStart;
    }

    public void setWorkingHoursStart(LocalTime workingHoursStart) {
        this.workingHoursStart = workingHoursStart;
    }

    public LocalTime getWorkingHoursEnd() {
        return workingHoursEnd;
    }

    public void setWorkingHoursEnd(LocalTime workingHoursEnd) {
        this.workingHoursEnd = workingHoursEnd;
    }

    public Integer getAppointmentSlotDurationMinutes() {
        return appointmentSlotDurationMinutes;
    }

    public void setAppointmentSlotDurationMinutes(Integer appointmentSlotDurationMinutes) {
        this.appointmentSlotDurationMinutes = appointmentSlotDurationMinutes;
    }

    public Integer getMaxAdvanceBookingDays() {
        return maxAdvanceBookingDays;
    }

    public void setMaxAdvanceBookingDays(Integer maxAdvanceBookingDays) {
        this.maxAdvanceBookingDays = maxAdvanceBookingDays;
    }

    public String getEmergencyContactNumber() {
        return emergencyContactNumber;
    }

    public void setEmergencyContactNumber(String emergencyContactNumber) {
        this.emergencyContactNumber = emergencyContactNumber;
    }

    public String getSupportEmail() {
        return supportEmail;
    }

    public void setSupportEmail(String supportEmail) {
        this.supportEmail = supportEmail;
    }
}
