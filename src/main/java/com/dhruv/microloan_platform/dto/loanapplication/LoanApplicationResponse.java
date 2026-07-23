package com.dhruv.microloan_platform.dto.loanapplication;

import com.dhruv.microloan_platform.entity.ApplicationStatus;
import com.dhruv.microloan_platform.entity.LoanApplication;

import java.math.BigDecimal;
import java.time.Instant;

public record LoanApplicationResponse(
        Long id,
        Long borrowerId,
        Long productId,
        BigDecimal requestedAmount,
        int requestedTenureMonths,
        ApplicationStatus status,
        String rejectionReason,
        Instant createdAt,
        Instant updatedAt
) {

    public static LoanApplicationResponse from(LoanApplication application) {
        return new LoanApplicationResponse(
                application.getId(),
                application.getBorrowerId(),
                application.getProductId(),
                application.getRequestedAmount(),
                application.getRequestedTenureMonths(),
                application.getStatus(),
                application.getRejectionReason(),
                application.getCreatedAt(),
                application.getUpdatedAt());
    }
}
