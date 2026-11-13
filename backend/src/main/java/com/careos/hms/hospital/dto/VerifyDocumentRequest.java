package com.careos.hms.hospital.dto;

import com.careos.hms.hospital.entity.DocumentVerificationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record VerifyDocumentRequest(
        @NotNull(message = "Verification status is required")
        DocumentVerificationStatus verificationStatus,

        @Size(max = 255, message = "Rejection reason cannot exceed 255 characters")
        String rejectionReason
) {}
