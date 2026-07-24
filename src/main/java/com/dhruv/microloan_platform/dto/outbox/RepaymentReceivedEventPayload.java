package com.dhruv.microloan_platform.dto.outbox;

import java.math.BigDecimal;
import java.time.Instant;

/** Serialized into OutboxEvent.payload for a REPAYMENT_RECEIVED event. */
public record RepaymentReceivedEventPayload(
        Long repaymentId,
        Long loanId,
        BigDecimal amount,
        BigDecimal balanceAfter,
        Instant paidAt
) {
}
