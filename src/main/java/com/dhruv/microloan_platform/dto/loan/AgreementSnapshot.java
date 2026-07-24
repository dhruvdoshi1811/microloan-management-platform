package com.dhruv.microloan_platform.dto.loan;

import java.math.BigDecimal;
import java.time.Instant;

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
