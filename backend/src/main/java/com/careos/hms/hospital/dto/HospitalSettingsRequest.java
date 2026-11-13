package com.careos.hms.hospital.dto;

import com.careos.hms.hospital.entity.HospitalSettings;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

public record HospitalSettingsRequest(
        @NotBlank(message = "Timezone is required")
        @Size(max = 64)
        String timezone,

        @NotBlank(message = "Currency is required")
        @Size(max = 10)
        String currency,

        @NotBlank(message = "Date format is required")
        @Size(max = 32)
        String dateFormat,

        @NotBlank(message = "Time format is required")
        @Size(max = 16)
        String timeFormat,

        @NotBlank(message = "Working days list is required")
        @Size(max = 100)
        String workingDays,

        @NotNull(message = "Working hours start is required")
        LocalTime workingHoursStart,

        @NotNull(message = "Working hours end is required")
        LocalTime workingHoursEnd,

        @NotNull(message = "Appointment slot duration is required")
        @Min(5) @Max(120)
        Integer appointmentSlotDurationMinutes,

        @NotNull(message = "Max advance booking days is required")
        @Min(1) @Max(365)
        Integer maxAdvanceBookingDays,

        @Size(max = 32)
        String emergencyContactNumber,

        @Size(max = 160)
        String supportEmail
) {}
