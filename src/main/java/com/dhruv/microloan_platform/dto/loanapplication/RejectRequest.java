package com.dhruv.microloan_platform.dto.loanapplication;

import jakarta.validation.constraints.NotBlank;

public record RejectRequest(

        @NotBlank(message = "Rejection reason is required")
        String rejectionReason
) {
}
