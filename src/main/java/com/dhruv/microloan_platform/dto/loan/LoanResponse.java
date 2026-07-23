package com.dhruv.microloan_platform.dto.loan;

import com.dhruv.microloan_platform.entity.Loan;
import com.dhruv.microloan_platform.entity.LoanStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record LoanResponse(
        Long id,
        Long borrowerId,
        Long applicationId,
        BigDecimal principalAmount,
        BigDecimal interestRate,
        int tenureMonths,
        BigDecimal emiAmount,
        BigDecimal totalPayable,
        BigDecimal totalPaid,
        LoanStatus status,
        String agreementSnapshot,
        Instant agreementAcknowledgedAt,
        Instant disbursedAt,
        Instant createdAt,
        Instant updatedAt
) {

    public static LoanResponse from(Loan loan) {
        return new LoanResponse(
                loan.getId(),
                loan.getBorrowerId(),
                loan.getApplicationId(),
                loan.getPrincipalAmount(),
                loan.getInterestRate(),
                loan.getTenureMonths(),
                loan.getEmiAmount(),
                loan.getTotalPayable(),
                loan.getTotalPaid(),
                loan.getStatus(),
                loan.getAgreementSnapshot(),
                loan.getAgreementAcknowledgedAt(),
                loan.getDisbursedAt(),
                loan.getCreatedAt(),
                loan.getUpdatedAt());
    }
}
