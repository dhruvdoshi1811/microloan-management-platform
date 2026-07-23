package com.dhruv.microloan_platform.dto.repayment;

import com.dhruv.microloan_platform.entity.Repayment;

import java.math.BigDecimal;
import java.time.Instant;

public record RepaymentResponse(
        Long id,
        Long loanId,
        BigDecimal amount,
        String paymentReference,
        String paymentMode,
        BigDecimal balanceAfter,
        Instant paidAt
) {

    public static RepaymentResponse from(Repayment repayment) {
        return new RepaymentResponse(
                repayment.getId(),
                repayment.getLoanId(),
                repayment.getAmount(),
                repayment.getPaymentReference(),
                repayment.getPaymentMode(),
                repayment.getBalanceAfter(),
                repayment.getPaidAt());
    }
}
