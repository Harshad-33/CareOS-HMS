package com.careos.hms.hr.leave.dto;

import jakarta.validation.constraints.Size;

public record ApproveLeaveStepRequest(
        @Size(max = 255, message = "Comments must not exceed 255 characters")
        String comments
) {
}
