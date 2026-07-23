package com.dhruv.microloan_platform.dto.loanapplication;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * borrowerId is a body field, not derived from the caller's JWT - User and Borrower are
 * still unlinked (Phase A decision), so the caller must say which borrower this application
 * is for.
 */
public record LoanApplicationRequest(

        @NotNull(message = "Borrower id is required")
        Long borrowerId,

        @NotNull(message = "Product id is required")
        Long productId,

        @NotNull(message = "Requested amount is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Requested amount must be positive")
        BigDecimal requestedAmount,

        @NotNull(message = "Requested tenure is required")
        @Positive(message = "Requested tenure must be positive")
        Integer requestedTenureMonths
) {
}
