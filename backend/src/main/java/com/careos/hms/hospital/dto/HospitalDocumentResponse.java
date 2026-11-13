package com.careos.hms.hospital.dto;

import com.careos.hms.hospital.entity.DocumentVerificationStatus;
import com.careos.hms.hospital.entity.HospitalDocument;

import java.time.Instant;
import java.time.LocalDate;

public record HospitalDocumentResponse(
        String publicId,
        String documentType,
        String documentName,
        String fileKey,
        String contentType,
        Long fileSizeBytes,
        Instant uploadDate,
        LocalDate expiryDate,
        DocumentVerificationStatus verificationStatus,
        Instant verifiedAt,
        String verifiedBy,
        String rejectionReason
) {
    public static HospitalDocumentResponse from(HospitalDocument d) {
        return new HospitalDocumentResponse(
                d.getPublicId(),
                d.getDocumentType(),
                d.getDocumentName(),
                d.getFileKey(),
                d.getContentType(),
                d.getFileSizeBytes(),
                d.getUploadDate(),
                d.getExpiryDate(),
                d.getVerificationStatus(),
                d.getVerifiedAt(),
                d.getVerifiedBy(),
                d.getRejectionReason()
        );
    }
}
