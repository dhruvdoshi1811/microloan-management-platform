package com.dhruv.microloan_platform.dto.outbox;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record LoanOverdueEventPayload(
        Long loanId,
        List<Integer> newlyOverdueInstallmentNumbers,
        BigDecimal totalPenaltyApplied,
        Instant detectedAt
) {
}
