package com.dhruv.microloan_platform.dto.repayment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** paymentMode is a free-form string - the spec doesn't enumerate a fixed set of values for it. */
public record RepaymentRequest(

        @NotNull(message = "Loan id is required")
        Long loanId,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be positive")
        BigDecimal amount,

        @NotBlank(message = "Payment reference is required")
        String paymentReference,

        @NotBlank(message = "Payment mode is required")
        String paymentMode
) {
}
