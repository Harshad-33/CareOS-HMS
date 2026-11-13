package com.careos.hms.hospital.dto;

import com.careos.hms.hospital.entity.Hospital;
import com.careos.hms.hospital.entity.HospitalStatus;
import com.careos.hms.hospital.entity.HospitalType;

import java.time.Instant;
import java.time.LocalDate;

public record HospitalResponse(
        Long id,
        String publicId,
        String hospitalCode,
        String registrationNumber,
        String name,
        HospitalType hospitalType,
        String email,
        String primaryContactNumber,
        String alternateContactNumber,
        String website,
        String addressLine1,
        String addressLine2,
        String city,
        String district,
        String state,
        String country,
        String pinCode,
        LocalDate establishedDate,
        HospitalStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static HospitalResponse from(Hospital h) {
        return new HospitalResponse(
                h.getId(),
                h.getPublicId(),
                h.getHospitalCode(),
                h.getRegistrationNumber(),
                h.getName(),
                h.getHospitalType(),
                h.getEmail(),
                h.getPrimaryContactNumber(),
                h.getAlternateContactNumber(),
                h.getWebsite(),
                h.getAddressLine1(),
                h.getAddressLine2(),
                h.getCity(),
                h.getDistrict(),
                h.getState(),
                h.getCountry(),
                h.getPinCode(),
                h.getEstablishedDate(),
                h.getStatus(),
                h.getCreatedAt(),
                h.getUpdatedAt()
        );
    }
}
