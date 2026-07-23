package com.dhruv.microloan_platform.dto.kyc;

import com.dhruv.microloan_platform.entity.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OtpVerifyRequest(

        @NotNull(message = "Document type is required")
        DocumentType documentType,

        @NotBlank(message = "OTP code is required")
        String otpCode
) {
}
