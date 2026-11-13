package com.careos.hms.department.dto;

import com.careos.hms.department.entity.Department;
import com.careos.hms.department.entity.DepartmentStatus;

import java.time.Instant;

public record DepartmentResponse(
        Long id,
        String publicId,
        String departmentCode,
        String departmentName,
        String description,
        DepartmentStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static DepartmentResponse from(Department d) {
        return new DepartmentResponse(
                d.getId(),
                d.getPublicId(),
                d.getDepartmentCode(),
                d.getDepartmentName(),
                d.getDescription(),
                d.getStatus(),
                d.getCreatedAt(),
                d.getUpdatedAt()
        );
    }
}
