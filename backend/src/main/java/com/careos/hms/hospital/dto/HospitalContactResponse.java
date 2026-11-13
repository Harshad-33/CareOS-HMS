package com.careos.hms.hospital.dto;

import com.careos.hms.hospital.entity.ContactType;
import com.careos.hms.hospital.entity.HospitalContact;

import java.time.Instant;

public record HospitalContactResponse(
        String publicId,
        ContactType contactType,
        String name,
        String designation,
        String phoneNumber,
        String email,
        boolean primary,
        Instant createdAt,
        Instant updatedAt
) {
    public static HospitalContactResponse from(HospitalContact c) {
        return new HospitalContactResponse(
                c.getPublicId(),
                c.getContactType(),
                c.getName(),
                c.getDesignation(),
                c.getPhoneNumber(),
                c.getEmail(),
                c.isPrimary(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }
}
