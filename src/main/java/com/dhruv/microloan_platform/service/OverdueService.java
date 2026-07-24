package com.dhruv.microloan_platform.service;

import com.dhruv.microloan_platform.dto.outbox.LoanOverdueEventPayload;
import com.dhruv.microloan_platform.dto.outbox.OverdueCheckResult;
import com.dhruv.microloan_platform.entity.Installment;
import com.dhruv.microloan_platform.entity.InstallmentStatus;
import com.dhruv.microloan_platform.entity.Loan;
import com.dhruv.microloan_platform.entity.LoanStatus;
import com.dhruv.microloan_platform.exception.ResourceNotFoundException;
import com.dhruv.microloan_platform.repository.InstallmentRepository;
import com.dhruv.microloan_platform.repository.LoanRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * The daily overdue-detection + idempotent-penalty batch job (also triggerable on demand via
 * POST /admin/run-overdue-check). One transaction for the whole run (not per-page or
 * per-loan) - simpler than chasing genuine per-page transactions, which would need either a
 * second bean or Spring's self-injection trick to dodge the proxy self-invocation pitfall,
 * and nothing here calls a flaky external system that would need per-record failure
 * isolation. Reuses Phase D's locked finders (LoanRepository.findByIdForUpdate,
 * InstallmentRepository.findUnpaidByLoanIdForUpdate) per loan, since this writes to the same
 * Installment rows RepaymentService does.
 */
@Service
public class OverdueService {

    private static final int PAGE_SIZE = 20;

    private final LoanRepository loanRepository;
    private final InstallmentRepository installmentRepository;
    private final OutboxEventWriter outboxEventWriter;

    public OverdueService(LoanRepository loanRepository, InstallmentRepository installmentRepository,
                           OutboxEventWriter outboxEventWriter) {
        this.loanRepository = loanRepository;
        this.installmentRepository = installmentRepository;
        this.outboxEventWriter = outboxEventWriter;
    }

    @Transactional
    public OverdueCheckResult runOverdueCheck() {
        LocalDate today = LocalDate.now();
        List<LoanStatus> eligibleStatuses = List.of(LoanStatus.ACTIVE, LoanStatus.OVERDUE);

        int loansScanned = 0;
        int loansMarkedOverdue = 0;
        int installmentsMarkedOverdue = 0;
        int penaltiesApplied = 0;

        Pageable pageable = PageRequest.of(0, PAGE_SIZE);
        Page<Loan> page;
        do {
            page = loanRepository.findByStatusInOrderByIdAsc(eligibleStatuses, pageable);

            for (Loan loanSummary : page) {
                loansScanned++;
                LoanOverdueOutcome outcome = processLoan(loanSummary.getId(), today);
                if (outcome.loanMarkedOverdue()) {
                    loansMarkedOverdue++;
                }
                installmentsMarkedOverdue += outcome.newlyOverdueCount();
                penaltiesApplied += outcome.penaltiesAppliedCount();
            }

            pageable = pageable.next();
        } while (page.hasNext());

        return new OverdueCheckResult(loansScanned, loansMarkedOverdue, installmentsMarkedOverdue, penaltiesApplied);
    }

    private LoanOverdueOutcome processLoan(Long loanId, LocalDate today) {
        Loan loan = loanRepository.findByIdForUpdate(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan " + loanId + " not found"));
        List<Installment> unpaidInstallments = installmentRepository.findUnpaidByLoanIdForUpdate(loanId, InstallmentStatus.PAID);

        List<Integer> newlyOverdueInstallmentNumbers = new ArrayList<>();
        BigDecimal totalPenaltyAppliedThisLoan = BigDecimal.ZERO;
        int penaltiesAppliedThisLoan = 0;

        for (Installment installment : unpaidInstallments) {
            if (!installment.getDueDate().isBefore(today)) {
                continue;
            }

            if (installment.getStatus() != InstallmentStatus.OVERDUE) {
                installment.setStatus(InstallmentStatus.OVERDUE);
                newlyOverdueInstallmentNumbers.add(installment.getInstallmentNo());
            }

            if (!installment.isPenaltyApplied()) {
                BigDecimal outstanding = installment.getTotalDue().subtract(installment.getAmountPaid());
                BigDecimal penalty = outstanding.multiply(loan.getPenaltyRate())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                installment.setPenaltyAmount(installment.getPenaltyAmount().add(penalty));
                installment.setTotalDue(installment.getTotalDue().add(penalty));
                installment.setPenaltyApplied(true);

                totalPenaltyAppliedThisLoan = totalPenaltyAppliedThisLoan.add(penalty);
                penaltiesAppliedThisLoan++;
            }
        }
        installmentRepository.saveAll(unpaidInstallments);

        boolean loanMarkedOverdue = false;
        if (!newlyOverdueInstallmentNumbers.isEmpty()) {
            if (loan.getStatus() != LoanStatus.OVERDUE) {
                loan.setStatus(LoanStatus.OVERDUE);
                loanMarkedOverdue = true;
            }
            loanRepository.save(loan);

            outboxEventWriter.write("LOAN", loan.getId(), "LOAN_OVERDUE",
                    new LoanOverdueEventPayload(loan.getId(), newlyOverdueInstallmentNumbers,
                            totalPenaltyAppliedThisLoan, Instant.now()));
        }

        return new LoanOverdueOutcome(loanMarkedOverdue, newlyOverdueInstallmentNumbers.size(), penaltiesAppliedThisLoan);
    }

    private record LoanOverdueOutcome(boolean loanMarkedOverdue, int newlyOverdueCount, int penaltiesAppliedCount) {
    }
}
