package com.dhruv.microloan_platform.service;

import com.dhruv.microloan_platform.dto.outbox.RepaymentReceivedEventPayload;
import com.dhruv.microloan_platform.dto.repayment.RepaymentRequest;
import com.dhruv.microloan_platform.dto.repayment.RepaymentResponse;
import com.dhruv.microloan_platform.entity.Installment;
import com.dhruv.microloan_platform.entity.InstallmentStatus;
import com.dhruv.microloan_platform.entity.Loan;
import com.dhruv.microloan_platform.entity.LoanStatus;
import com.dhruv.microloan_platform.entity.Repayment;
import com.dhruv.microloan_platform.exception.BusinessRuleException;
import com.dhruv.microloan_platform.exception.ResourceNotFoundException;
import com.dhruv.microloan_platform.repository.InstallmentRepository;
import com.dhruv.microloan_platform.repository.LoanRepository;
import com.dhruv.microloan_platform.repository.RepaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * The pessimistic-locking core of the platform. processRepayment locks the Loan row and
 * every unpaid Installment row for it (both PESSIMISTIC_WRITE, i.e. SELECT ... FOR UPDATE)
 * before allocating anything - chosen over @Version here specifically because retrying a
 * failed multi-row FIFO allocation is expensive and confusing (it's money, and there's no
 * way to "resume" a partial allocation), whereas a short wait behind a lock is not. Contrast
 * with LoanApplication/Loan's @Version in Phase C, where conflicts are rare and a retry is cheap.
 */
@Service
public class RepaymentService {

    private final RepaymentRepository repaymentRepository;
    private final LoanRepository loanRepository;
    private final InstallmentRepository installmentRepository;
    private final OutboxEventWriter outboxEventWriter;

    public RepaymentService(RepaymentRepository repaymentRepository, LoanRepository loanRepository,
                             InstallmentRepository installmentRepository, OutboxEventWriter outboxEventWriter) {
        this.repaymentRepository = repaymentRepository;
        this.loanRepository = loanRepository;
        this.installmentRepository = installmentRepository;
        this.outboxEventWriter = outboxEventWriter;
    }

    @Transactional
    public RepaymentResponse processRepayment(RepaymentRequest request) {
        Loan loan = loanRepository.findByIdForUpdate(request.loanId())
                .orElseThrow(() -> new ResourceNotFoundException("Loan " + request.loanId() + " not found"));

        // Authoritative idempotency check, done AFTER acquiring the loan lock: two concurrent
        // requests with the same paymentReference for this loan serialize on that lock, so by
        // the time the second one reaches this line, the first's Repayment row (if any) is
        // already committed and visible. A pre-lock check would only be a fast-path
        // optimization, never required for correctness - so there's just this one check.
        Optional<Repayment> existing = repaymentRepository.findByPaymentReference(request.paymentReference());
        if (existing.isPresent()) {
            return RepaymentResponse.from(existing.get());
        }

        if (loan.getStatus() != LoanStatus.ACTIVE && loan.getStatus() != LoanStatus.OVERDUE) {
            throw new BusinessRuleException("Loan " + loan.getId() + " is not accepting repayments (current status: "
                    + loan.getStatus() + ")");
        }

        BigDecimal outstanding = loan.getTotalPayable().subtract(loan.getTotalPaid());
        if (request.amount().compareTo(outstanding) > 0) {
            throw new BusinessRuleException("Repayment amount " + request.amount()
                    + " exceeds outstanding balance " + outstanding);
        }

        List<Installment> unpaidInstallments = installmentRepository.findUnpaidByLoanIdForUpdate(loan.getId(), InstallmentStatus.PAID);
        allocateFifo(request.amount(), unpaidInstallments);
        installmentRepository.saveAll(unpaidInstallments);

        loan.setTotalPaid(loan.getTotalPaid().add(request.amount()));
        if (loan.getTotalPaid().compareTo(loan.getTotalPayable()) >= 0) {
            loan.setStatus(LoanStatus.CLOSED);
        }
        loanRepository.save(loan);

        Repayment repayment = Repayment.builder()
                .loanId(loan.getId())
                .amount(request.amount())
                .paymentReference(request.paymentReference())
                .paymentMode(request.paymentMode())
                .balanceAfter(loan.getTotalPayable().subtract(loan.getTotalPaid()))
                .build();
        Repayment savedRepayment = repaymentRepository.save(repayment);

        outboxEventWriter.write("LOAN", loan.getId(), "REPAYMENT_RECEIVED",
                new RepaymentReceivedEventPayload(savedRepayment.getId(), loan.getId(),
                        savedRepayment.getAmount(), savedRepayment.getBalanceAfter(), savedRepayment.getPaidAt()));

        return RepaymentResponse.from(savedRepayment);
    }

    public RepaymentResponse get(Long id) {
        return RepaymentResponse.from(repaymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Repayment " + id + " not found")));
    }

    public List<RepaymentResponse> getByLoan(Long loanId) {
        if (!loanRepository.existsById(loanId)) {
            throw new ResourceNotFoundException("Loan " + loanId + " not found");
        }
        return repaymentRepository.findByLoanIdOrderByPaidAtAsc(loanId).stream()
                .map(RepaymentResponse::from)
                .toList();
    }

    /** Applies {@code amount} across installments in the order given (FIFO), oldest first. */
    private void allocateFifo(BigDecimal amount, List<Installment> installmentsInFifoOrder) {
        BigDecimal remaining = amount;

        for (Installment installment : installmentsInFifoOrder) {
            if (remaining.signum() == 0) {
                break;
            }

            BigDecimal due = installment.getTotalDue().subtract(installment.getAmountPaid());
            BigDecimal toApply = remaining.min(due);

            installment.setAmountPaid(installment.getAmountPaid().add(toApply));
            installment.setStatus(installment.getAmountPaid().compareTo(installment.getTotalDue()) >= 0
                    ? InstallmentStatus.PAID
                    : InstallmentStatus.PARTIAL);

            remaining = remaining.subtract(toApply);
        }
    }
}
