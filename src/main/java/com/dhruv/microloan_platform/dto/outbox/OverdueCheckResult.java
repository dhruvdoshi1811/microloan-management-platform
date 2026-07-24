package com.dhruv.microloan_platform.dto.outbox;

public record OverdueCheckResult(
        int loansScanned,
        int loansMarkedOverdue,
        int installmentsMarkedOverdue,
        int penaltiesApplied
) {
}
