package com.careos.hms.hospital.dto;

import com.careos.hms.hospital.entity.HospitalSettings;

import java.time.Instant;
import java.time.LocalTime;

public record HospitalSettingsResponse(
        Long id,
        String timezone,
        String currency,
        String dateFormat,
        String timeFormat,
        String workingDays,
        LocalTime workingHoursStart,
        LocalTime workingHoursEnd,
        Integer appointmentSlotDurationMinutes,
        Integer maxAdvanceBookingDays,
        String emergencyContactNumber,
        String supportEmail,
        Instant updatedAt
) {
    public static HospitalSettingsResponse from(HospitalSettings s) {
        return new HospitalSettingsResponse(
                s.getId(),
                s.getTimezone(),
                s.getCurrency(),
                s.getDateFormat(),
                s.getTimeFormat(),
                s.getWorkingDays(),
                s.getWorkingHoursStart(),
                s.getWorkingHoursEnd(),
                s.getAppointmentSlotDurationMinutes(),
                s.getMaxAdvanceBookingDays(),
                s.getEmergencyContactNumber(),
                s.getSupportEmail(),
                s.getUpdatedAt()
        );
    }
}
