package com.careos.hms.hr.leave.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectLeaveStepRequest(
        @NotBlank(message = "Rejection reason is mandatory")
        @Size(max = 255, message = "Rejection reason must not exceed 255 characters")
        String rejectionReason,

        @Size(max = 255, message = "Comments must not exceed 255 characters")
        String comments
) {
}
