package com.careos.hms.branch.dto;

import com.careos.hms.branch.entity.BranchStatus;
import com.careos.hms.branch.entity.HospitalBranch;

import java.time.Instant;

public record HospitalBranchResponse(
        Long id,
        String publicId,
        String branchCode,
        String branchName,
        String contactNumber,
        String email,
        String address,
        String city,
        String district,
        String state,
        String country,
        String pinCode,
        BranchStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static HospitalBranchResponse from(HospitalBranch b) {
        return new HospitalBranchResponse(
                b.getId(),
                b.getPublicId(),
                b.getBranchCode(),
                b.getBranchName(),
                b.getContactNumber(),
                b.getEmail(),
                b.getAddress(),
                b.getCity(),
                b.getDistrict(),
                b.getState(),
                b.getCountry(),
                b.getPinCode(),
                b.getStatus(),
                b.getCreatedAt(),
                b.getUpdatedAt()
        );
    }
}
