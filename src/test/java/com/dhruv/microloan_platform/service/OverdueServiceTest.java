package com.dhruv.microloan_platform.service;

import com.dhruv.microloan_platform.dto.outbox.OverdueCheckResult;
import com.dhruv.microloan_platform.entity.Installment;
import com.dhruv.microloan_platform.entity.InstallmentStatus;
import com.dhruv.microloan_platform.entity.Loan;
import com.dhruv.microloan_platform.entity.LoanStatus;
import com.dhruv.microloan_platform.repository.InstallmentRepository;
import com.dhruv.microloan_platform.repository.LoanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OverdueServiceTest {

    @Mock
    private LoanRepository loanRepository;
    @Mock
    private InstallmentRepository installmentRepository;
    @Mock
    private OutboxEventWriter outboxEventWriter;

    @InjectMocks
    private OverdueService overdueService;

    private static final Long LOAN_ID = 1L;

    private Loan activeLoan() {
        return activeLoanWithId(LOAN_ID);
    }

    private Loan activeLoanWithId(Long id) {
        return Loan.builder()
                .id(id)
                .borrowerId(2L)
                .applicationId(3L)
                .principalAmount(new BigDecimal("100000.00"))
                .interestRate(new BigDecimal("12.00"))
                .penaltyRate(new BigDecimal("2.00"))
                .tenureMonths(10)
                .emiAmount(new BigDecimal("20000.00"))
                .totalPayable(new BigDecimal("200000.00"))
                .totalPaid(BigDecimal.ZERO)
                .agreementSnapshot("{}")
                .status(LoanStatus.ACTIVE)
                .build();
    }

    private Installment overdueEligibleInstallment(InstallmentStatus status, boolean penaltyApplied) {
        return Installment.builder()
                .id(10L)
                .loanId(LOAN_ID)
                .installmentNo(1)
                .dueDate(LocalDate.now().minusDays(5))
                .emiAmount(new BigDecimal("20000.00"))
                .totalDue(new BigDecimal("20000.00"))
                .status(status)
                .penaltyApplied(penaltyApplied)
                .build();
    }

    private void stubSinglePage(Loan loan) {
        Pageable pageable = PageRequest.of(0, 20);
        when(loanRepository.findByStatusInOrderByIdAsc(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(loan), pageable, 1));
    }

    @Test
    void marksNewlyOverdueInstallmentAndAppliesPenaltyOnce() {
        Loan loan = activeLoan();
        stubSinglePage(loan);
        when(loanRepository.findByIdForUpdate(LOAN_ID)).thenReturn(Optional.of(loan));
        Installment installment = overdueEligibleInstallment(InstallmentStatus.PENDING, false);
        when(installmentRepository.findUnpaidByLoanIdForUpdate(LOAN_ID, InstallmentStatus.PAID))
                .thenReturn(List.of(installment));

        OverdueCheckResult result = overdueService.runOverdueCheck();

        assertThat(installment.getStatus()).isEqualTo(InstallmentStatus.OVERDUE);
        assertThat(installment.isPenaltyApplied()).isTrue();
        assertThat(installment.getPenaltyAmount()).isEqualByComparingTo("400.00");
        assertThat(installment.getTotalDue()).isEqualByComparingTo("20400.00");
        assertThat(loan.getStatus()).isEqualTo(LoanStatus.OVERDUE);

        assertThat(result.loansScanned()).isEqualTo(1);
        assertThat(result.loansMarkedOverdue()).isEqualTo(1);
        assertThat(result.installmentsMarkedOverdue()).isEqualTo(1);
        assertThat(result.penaltiesApplied()).isEqualTo(1);

        verify(outboxEventWriter).write(any(), any(), eq("LOAN_OVERDUE"), any());
    }

    @Test
    void skipsInstallmentNotYetPastDue() {
        Loan loan = activeLoan();
        stubSinglePage(loan);
        when(loanRepository.findByIdForUpdate(LOAN_ID)).thenReturn(Optional.of(loan));
        Installment notYetDue = Installment.builder()
                .id(11L).loanId(LOAN_ID).installmentNo(1)
                .dueDate(LocalDate.now())
                .emiAmount(new BigDecimal("20000.00")).totalDue(new BigDecimal("20000.00"))
                .build();
        when(installmentRepository.findUnpaidByLoanIdForUpdate(LOAN_ID, InstallmentStatus.PAID))
                .thenReturn(List.of(notYetDue));

        OverdueCheckResult result = overdueService.runOverdueCheck();

        assertThat(notYetDue.getStatus()).isEqualTo(InstallmentStatus.PENDING);
        assertThat(result.installmentsMarkedOverdue()).isZero();
        assertThat(result.penaltiesApplied()).isZero();
        verify(loanRepository, never()).save(any());
        verify(outboxEventWriter, never()).write(any(), any(), any(), any());
    }

    @Test
    void doesNotReChargePenaltyOrReNotifyOnAlreadyOverdueInstallment() {
        Loan loan = activeLoan();
        loan.setStatus(LoanStatus.OVERDUE);
        stubSinglePage(loan);
        when(loanRepository.findByIdForUpdate(LOAN_ID)).thenReturn(Optional.of(loan));
        Installment alreadyProcessed = overdueEligibleInstallment(InstallmentStatus.OVERDUE, true);
        alreadyProcessed.setPenaltyAmount(new BigDecimal("400.00"));
        alreadyProcessed.setTotalDue(new BigDecimal("20400.00"));
        when(installmentRepository.findUnpaidByLoanIdForUpdate(LOAN_ID, InstallmentStatus.PAID))
                .thenReturn(List.of(alreadyProcessed));

        OverdueCheckResult result = overdueService.runOverdueCheck();

        assertThat(alreadyProcessed.getPenaltyAmount()).isEqualByComparingTo("400.00");
        assertThat(alreadyProcessed.getTotalDue()).isEqualByComparingTo("20400.00");
        assertThat(result.installmentsMarkedOverdue()).isZero();
        assertThat(result.penaltiesApplied()).isZero();
        verify(loanRepository, never()).save(any());
        verify(outboxEventWriter, never()).write(any(), any(), any(), any());
    }

    @Test
    void pagesThroughMultiplePagesOfLoans() {
        List<Loan> page0Loans = buildLoans(1, 20);
        List<Loan> page1Loans = buildLoans(21, 5);

        Pageable pageable0 = PageRequest.of(0, 20);
        Pageable pageable1 = pageable0.next();
        when(loanRepository.findByStatusInOrderByIdAsc(any(), eq(pageable0)))
                .thenReturn(new PageImpl<>(page0Loans, pageable0, 25));
        when(loanRepository.findByStatusInOrderByIdAsc(any(), eq(pageable1)))
                .thenReturn(new PageImpl<>(page1Loans, pageable1, 25));

        when(loanRepository.findByIdForUpdate(any()))
                .thenAnswer(inv -> Optional.of(activeLoanWithId(inv.getArgument(0))));
        when(installmentRepository.findUnpaidByLoanIdForUpdate(any(), any())).thenReturn(List.of());

        OverdueCheckResult result = overdueService.runOverdueCheck();

        assertThat(result.loansScanned()).isEqualTo(25);
        verify(loanRepository).findByStatusInOrderByIdAsc(any(), eq(pageable0));
        verify(loanRepository).findByStatusInOrderByIdAsc(any(), eq(pageable1));
    }

    private List<Loan> buildLoans(long startId, int count) {
        List<Loan> loans = new ArrayList<>();
        for (long i = startId; i < startId + count; i++) {
            loans.add(activeLoanWithId(i));
        }
        return loans;
    }
}
