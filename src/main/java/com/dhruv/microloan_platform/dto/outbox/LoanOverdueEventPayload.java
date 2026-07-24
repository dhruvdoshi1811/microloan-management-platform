package com.dhruv.microloan_platform.dto.outbox;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Serialized into OutboxEvent.payload for a LOAN_OVERDUE event - one per loan per run, not per installment. */
public record LoanOverdueEventPayload(
        Long loanId,
        List<Integer> newlyOverdueInstallmentNumbers,
        BigDecimal totalPenaltyApplied,
        Instant detectedAt
) {
}
