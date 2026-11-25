package com.careos.hms.employee.dto;

import com.careos.hms.employee.entity.EmployeeLifecycleEvent;
import com.careos.hms.employee.entity.EmployeeStatus;

import java.time.Instant;
import java.time.LocalDate;

public record EmployeeLifecycleEventResponse(
        String publicId,
        EmployeeStatus previousStatus,
        EmployeeStatus newStatus,
        String reason,
        LocalDate effectiveDate,
        String actorUsername,
        Instant createdAt
) {
    public static EmployeeLifecycleEventResponse from(EmployeeLifecycleEvent event) {
        return new EmployeeLifecycleEventResponse(
                event.getPublicId(),
                event.getPreviousStatus(),
                event.getNewStatus(),
                event.getReason(),
                event.getEffectiveDate(),
                event.getActorUsername(),
                event.getCreatedAt()
        );
    }
}
