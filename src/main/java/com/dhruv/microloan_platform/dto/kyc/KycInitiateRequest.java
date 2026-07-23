package com.dhruv.microloan_platform.dto.kyc;

import com.dhruv.microloan_platform.entity.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** documentNumber is the PAN or Aadhaar number itself, whichever documentType names. */
public record KycInitiateRequest(

        @NotNull(message = "Document type is required")
        DocumentType documentType,

        @NotBlank(message = "Document number is required")
        String documentNumber
) {
}
