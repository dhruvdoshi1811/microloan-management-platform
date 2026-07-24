package com.dhruv.microloan_platform.dto.outbox;

import java.math.BigDecimal;
import java.time.Instant;

public record RepaymentReceivedEventPayload(
        Long repaymentId,
        Long loanId,
        BigDecimal amount,
        BigDecimal balanceAfter,
        Instant paidAt
) {
}
