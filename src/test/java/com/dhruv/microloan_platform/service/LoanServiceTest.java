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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock
    private LoanRepository loanRepository;
    @Mock
    private InstallmentRepository installmentRepository;

    @InjectMocks
    private LoanService loanService;

    private Loan agreementPendingLoan(Long id) {
        return Loan.builder()
                .id(id)
                .borrowerId(1L)
                .applicationId(2L)
                .principalAmount(new BigDecimal("100000.00"))
                .interestRate(new BigDecimal("12.00"))
                .tenureMonths(12)
                .emiAmount(new BigDecimal("8884.88"))
                .totalPayable(new BigDecimal("106618.56"))
                .agreementSnapshot("{\"principal\":100000}")
                .status(LoanStatus.AGREEMENT_PENDING)
                .build();
    }

    @Test
    void getThrowsWhenMissing() {
        when(loanRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanService.get(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getInstallmentsThrowsWhenLoanMissing() {
        when(loanRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanService.getInstallments(99L)).isInstanceOf(ResourceNotFoundException.class);
        verify(installmentRepository, never()).findByLoanIdOrderByInstallmentNoAsc(any());
    }

    @Test
    void getInstallmentsReturnsThemInOrder() {
        when(loanRepository.findById(1L)).thenReturn(Optional.of(agreementPendingLoan(1L)));
        Installment first = Installment.builder().loanId(1L).installmentNo(1)
                .dueDate(java.time.LocalDate.now().plusMonths(1))
                .emiAmount(new BigDecimal("8884.88")).totalDue(new BigDecimal("8884.88")).build();
        when(installmentRepository.findByLoanIdOrderByInstallmentNoAsc(1L)).thenReturn(List.of(first));

        List<InstallmentResponse> installments = loanService.getInstallments(1L);

        assertThat(installments).hasSize(1);
        assertThat(installments.get(0).installmentNo()).isEqualTo(1);
    }

    @Test
    void acknowledgeAgreementThrowsWhenNotAgreementPending() {
        Loan loan = agreementPendingLoan(1L);
        loan.setStatus(LoanStatus.ACTIVE);
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));

        assertThatThrownBy(() -> loanService.acknowledgeAgreement(1L)).isInstanceOf(BusinessRuleException.class);
        verify(installmentRepository, never()).saveAll(anyList());
    }

    @Test
    void acknowledgeAgreementGeneratesFullScheduleAndActivatesLoan() {
        Loan loan = agreementPendingLoan(1L);
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(loanRepository.save(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));

        LoanResponse response = loanService.acknowledgeAgreement(1L);

        assertThat(response.status()).isEqualTo(LoanStatus.ACTIVE);
        assertThat(response.agreementAcknowledgedAt()).isNotNull();
        assertThat(response.disbursedAt()).isNotNull();

        ArgumentCaptor<List<Installment>> scheduleCaptor = ArgumentCaptor.forClass(List.class);
        verify(installmentRepository).saveAll(scheduleCaptor.capture());
        List<Installment> schedule = scheduleCaptor.getValue();

        assertThat(schedule).hasSize(12);
        assertThat(schedule).extracting(Installment::getInstallmentNo)
                .containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
        assertThat(schedule).allSatisfy(installment -> {
            assertThat(installment.getLoanId()).isEqualTo(1L);
            assertThat(installment.getEmiAmount()).isEqualByComparingTo("8884.88");
            assertThat(installment.getTotalDue()).isEqualByComparingTo("8884.88");
        });

        BigDecimal scheduleTotal = schedule.stream().map(Installment::getTotalDue).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(scheduleTotal).isEqualByComparingTo(loan.getTotalPayable());
    }
}
