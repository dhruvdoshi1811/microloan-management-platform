package com.dhruv.microloan_platform.service;

import com.dhruv.microloan_platform.dto.loan.InstallmentResponse;
import com.dhruv.microloan_platform.dto.loan.LoanResponse;
import com.dhruv.microloan_platform.entity.Installment;
import com.dhruv.microloan_platform.entity.Loan;
import com.dhruv.microloan_platform.entity.LoanStatus;
import com.dhruv.microloan_platform.exception.BusinessRuleException;
import com.dhruv.microloan_platform.exception.ResourceNotFoundException;
import com.dhruv.microloan_platform.repository.InstallmentRepository;
import com.dhruv.microloan_platform.repository.LoanRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Owns the Loan side of the lifecycle: reading a loan/its installments, and the
 * acknowledge-agreement transition that generates the installment schedule and disburses.
 * Doesn't touch LoanApplication/LoanProduct at all - by the time a Loan exists, its terms
 * are already frozen in agreementSnapshot, so nothing here needs either of those tables.
 */
@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final InstallmentRepository installmentRepository;

    public LoanService(LoanRepository loanRepository, InstallmentRepository installmentRepository) {
        this.loanRepository = loanRepository;
        this.installmentRepository = installmentRepository;
    }

    public LoanResponse get(Long id) {
        return LoanResponse.from(findOrThrow(id));
    }

    public Page<LoanResponse> list(Pageable pageable) {
        return loanRepository.findAll(pageable).map(LoanResponse::from);
    }

    public List<InstallmentResponse> getInstallments(Long loanId) {
        findOrThrow(loanId);
        return installmentRepository.findByLoanIdOrderByInstallmentNoAsc(loanId).stream()
                .map(InstallmentResponse::from)
                .toList();
    }

    @Transactional
    public LoanResponse acknowledgeAgreement(Long id) {
        Loan loan = findOrThrow(id);
        if (loan.getStatus() != LoanStatus.AGREEMENT_PENDING) {
            throw new BusinessRuleException("Loan " + id + " is not awaiting agreement (current status: "
                    + loan.getStatus() + ")");
        }

        installmentRepository.saveAll(buildSchedule(loan));

        Instant now = Instant.now();
        loan.setAgreementAcknowledgedAt(now);
        loan.setDisbursedAt(now);
        loan.setStatus(LoanStatus.ACTIVE);

        return LoanResponse.from(loanRepository.save(loan));
    }

    /**
     * Every installment carries the same EMI (totalPayable is defined as emi * tenureMonths,
     * see LoanApplicationService.buildLoan, so there's no separate "true" total that could
     * drift from n * emi to reconcile here - the schedule always sums exactly to totalPayable).
     */
    private List<Installment> buildSchedule(Loan loan) {
        List<Installment> installments = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int installmentNo = 1; installmentNo <= loan.getTenureMonths(); installmentNo++) {
            installments.add(Installment.builder()
                    .loanId(loan.getId())
                    .installmentNo(installmentNo)
                    .dueDate(today.plusMonths(installmentNo))
                    .emiAmount(loan.getEmiAmount())
                    .totalDue(loan.getEmiAmount())
                    .build());
        }

        return installments;
    }

    private Loan findOrThrow(Long id) {
        return loanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan " + id + " not found"));
    }
}
