package com.dhruv.microloan_platform.dto.loan;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * What gets frozen into Loan.agreementSnapshot (as JSON) the moment an application is
 * approved. Never re-read or reconstructed from LoanProduct afterwards - if the product's
 * rate changes next week, every Loan created before that keeps exactly these terms, because
 * nothing about this loan's repayment math ever looks at LoanProduct again.
 */
public record AgreementSnapshot(
        Long applicationId,
        Long productId,
        String productName,
        BigDecimal principalAmount,
        BigDecimal interestRate,
        BigDecimal penaltyRate,
        int tenureMonths,
        BigDecimal emiAmount,
        BigDecimal totalPayable,
        Instant frozenAt
) {
}
