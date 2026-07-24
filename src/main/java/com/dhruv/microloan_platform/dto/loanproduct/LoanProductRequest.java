package com.dhruv.microloan_platform.dto.loanproduct;

import com.dhruv.microloan_platform.entity.KycLevel;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record LoanProductRequest(

        @NotBlank(message = "Name is required")
        String name,

        @NotNull(message = "Minimum principal is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Minimum principal must be positive")
        BigDecimal minPrincipal,

        @NotNull(message = "Maximum principal is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Maximum principal must be positive")
        BigDecimal maxPrincipal,

        @NotNull(message = "Minimum tenure is required")
        @Min(value = 1, message = "Minimum tenure must be at least 1 month")
        Integer minTenureMonths,

        @NotNull(message = "Maximum tenure is required")
        @Min(value = 1, message = "Maximum tenure must be at least 1 month")
        Integer maxTenureMonths,

        @NotNull(message = "Interest rate is required")
        @DecimalMin(value = "0.0", message = "Interest rate cannot be negative")
        BigDecimal interestRate,

        @NotNull(message = "Penalty rate is required")
        @DecimalMin(value = "0.0", message = "Penalty rate cannot be negative")
        BigDecimal penaltyRate,

        @NotNull(message = "Minimum KYC level is required")
        KycLevel minKycLevel
) {
}
