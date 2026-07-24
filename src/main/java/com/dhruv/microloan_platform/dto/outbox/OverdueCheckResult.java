package com.dhruv.microloan_platform.dto.outbox;

/** Summary returned by POST /admin/run-overdue-check - the manual/demo trigger for OverdueService. */
public record OverdueCheckResult(
        int loansScanned,
        int loansMarkedOverdue,
        int installmentsMarkedOverdue,
        int penaltiesApplied
) {
}
