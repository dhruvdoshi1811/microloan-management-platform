package com.dhruv.microloan_platform.dto.outbox;

import java.math.BigDecimal;
import java.time.Instant;

/** Serialized into OutboxEvent.payload for a LOAN_APPROVED event. */
public record LoanApprovedEventPayload(
        Long applicationId,
        Long loanId,
        BigDecimal principalAmount,
        BigDecimal emiAmount,
        Instant approvedAt
) {
}
