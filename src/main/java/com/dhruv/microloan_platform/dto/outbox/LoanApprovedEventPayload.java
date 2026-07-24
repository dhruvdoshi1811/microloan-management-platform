package com.dhruv.microloan_platform.dto.outbox;

import java.math.BigDecimal;
import java.time.Instant;

public record LoanApprovedEventPayload(
        Long applicationId,
        Long loanId,
        BigDecimal principalAmount,
        BigDecimal emiAmount,
        Instant approvedAt
) {
}
