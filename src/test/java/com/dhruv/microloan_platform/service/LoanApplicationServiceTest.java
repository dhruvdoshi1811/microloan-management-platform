package com.dhruv.microloan_platform.service;

import com.dhruv.microloan_platform.dto.loanapplication.LoanApplicationRequest;
import com.dhruv.microloan_platform.dto.loanapplication.LoanApplicationResponse;
import com.dhruv.microloan_platform.dto.loanapplication.RejectRequest;
import com.dhruv.microloan_platform.entity.ApplicationStatus;
import com.dhruv.microloan_platform.entity.Borrower;
import com.dhruv.microloan_platform.entity.KycLevel;
import com.dhruv.microloan_platform.entity.Loan;
import com.dhruv.microloan_platform.entity.LoanApplication;
import com.dhruv.microloan_platform.entity.LoanProduct;
import com.dhruv.microloan_platform.entity.LoanStatus;
import com.dhruv.microloan_platform.exception.BusinessRuleException;
import com.dhruv.microloan_platform.exception.ResourceNotFoundException;
import com.dhruv.microloan_platform.repository.BorrowerRepository;
import com.dhruv.microloan_platform.repository.LoanApplicationRepository;
import com.dhruv.microloan_platform.repository.LoanProductRepository;
import com.dhruv.microloan_platform.repository.LoanRepository;
import com.dhruv.microloan_platform.service.eligibility.LoanEligibilityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanApplicationServiceTest {

    @Mock
    private LoanApplicationRepository loanApplicationRepository;
    @Mock
    private BorrowerRepository borrowerRepository;
    @Mock
    private LoanProductRepository loanProductRepository;
    @Mock
    private LoanRepository loanRepository;
    @Mock
    private LoanEligibilityService loanEligibilityService;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private OutboxEventWriter outboxEventWriter;

    @InjectMocks
    private LoanApplicationService loanApplicationService;

    private static final Long BORROWER_ID = 1L;
    private static final Long PRODUCT_ID = 2L;

    private Borrower borrower() {
        return Borrower.builder()
                .id(BORROWER_ID)
                .fullName("Alice")
                .phone("9999999999")
                .email("alice@example.com")
                .dob(LocalDate.of(1995, 1, 1))
                .monthlyIncome(new BigDecimal("50000"))
                .kycLevel(KycLevel.BASIC)
                .build();
    }

    private LoanProduct product() {
        return LoanProduct.builder()
                .id(PRODUCT_ID)
                .minPrincipal(new BigDecimal("10000"))
                .maxPrincipal(new BigDecimal("500000"))
                .minTenureMonths(6)
                .maxTenureMonths(36)
                .interestRate(new BigDecimal("12"))
                .penaltyRate(new BigDecimal("2"))
                .minKycLevel(KycLevel.BASIC)
                .build();
    }

    private LoanApplication pendingApplication(Long id) {
        return LoanApplication.builder()
                .id(id)
                .borrowerId(BORROWER_ID)
                .productId(PRODUCT_ID)
                .requestedAmount(new BigDecimal("100000"))
                .requestedTenureMonths(12)
                .status(ApplicationStatus.PENDING)
                .build();
    }

    @Test
    void submitRunsEligibilityThenSavesPendingApplication() {
        LoanApplicationRequest request = new LoanApplicationRequest(BORROWER_ID, PRODUCT_ID, new BigDecimal("100000"), 12);
        // Reused as the same references below: Borrower/LoanProduct have no equals(), so
        // verify(...) against freshly-built instances would fail on reference equality.
        Borrower borrower = borrower();
        LoanProduct product = product();
        when(borrowerRepository.findById(BORROWER_ID)).thenReturn(Optional.of(borrower));
        when(loanProductRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(loanApplicationRepository.save(any(LoanApplication.class))).thenAnswer(inv -> {
            LoanApplication saved = inv.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        LoanApplicationResponse response = loanApplicationService.submit(request);

        assertThat(response.status()).isEqualTo(ApplicationStatus.PENDING);
        verify(loanEligibilityService).checkEligibility(product, borrower, new BigDecimal("100000"), 12);
    }

    @Test
    void submitThrowsWhenBorrowerMissing() {
        LoanApplicationRequest request = new LoanApplicationRequest(BORROWER_ID, PRODUCT_ID, new BigDecimal("100000"), 12);
        when(borrowerRepository.findById(BORROWER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanApplicationService.submit(request)).isInstanceOf(ResourceNotFoundException.class);
        verify(loanApplicationRepository, never()).save(any());
    }

    @Test
    void submitThrowsWhenProductMissing() {
        LoanApplicationRequest request = new LoanApplicationRequest(BORROWER_ID, PRODUCT_ID, new BigDecimal("100000"), 12);
        when(borrowerRepository.findById(BORROWER_ID)).thenReturn(Optional.of(borrower()));
        when(loanProductRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanApplicationService.submit(request)).isInstanceOf(ResourceNotFoundException.class);
        verify(loanApplicationRepository, never()).save(any());
    }

    @Test
    void submitPropagatesEligibilityFailureWithoutSaving() {
        LoanApplicationRequest request = new LoanApplicationRequest(BORROWER_ID, PRODUCT_ID, new BigDecimal("100000"), 12);
        when(borrowerRepository.findById(BORROWER_ID)).thenReturn(Optional.of(borrower()));
        when(loanProductRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product()));
        doThrow(new BusinessRuleException("not eligible"))
                .when(loanEligibilityService).checkEligibility(any(), any(), any(), anyInt());

        assertThatThrownBy(() -> loanApplicationService.submit(request)).isInstanceOf(BusinessRuleException.class);
        verify(loanApplicationRepository, never()).save(any());
    }

    @Test
    void approveFlipsPendingToApprovedAndCreatesLoanWithFrozenSnapshot() {
        LoanApplication application = pendingApplication(5L);
        when(loanApplicationRepository.findById(5L)).thenReturn(Optional.of(application));
        when(loanProductRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product()));
        when(loanApplicationRepository.save(any(LoanApplication.class))).thenAnswer(inv -> inv.getArgument(0));
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"frozen\":true}");
        when(loanRepository.save(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));

        LoanApplicationResponse response = loanApplicationService.approve(5L);

        assertThat(response.status()).isEqualTo(ApplicationStatus.APPROVED);

        ArgumentCaptor<Loan> loanCaptor = ArgumentCaptor.forClass(Loan.class);
        verify(loanRepository).save(loanCaptor.capture());
        Loan createdLoan = loanCaptor.getValue();

        assertThat(createdLoan.getBorrowerId()).isEqualTo(BORROWER_ID);
        assertThat(createdLoan.getApplicationId()).isEqualTo(5L);
        assertThat(createdLoan.getStatus()).isEqualTo(LoanStatus.AGREEMENT_PENDING);
        assertThat(createdLoan.getPrincipalAmount()).isEqualByComparingTo("100000");
        assertThat(createdLoan.getInterestRate()).isEqualByComparingTo("12");
        assertThat(createdLoan.getTenureMonths()).isEqualTo(12);
        assertThat(createdLoan.getEmiAmount())
                .isEqualByComparingTo(EmiCalculator.calculateEmi(new BigDecimal("100000"), new BigDecimal("12"), 12));
        assertThat(createdLoan.getTotalPayable())
                .isEqualByComparingTo(createdLoan.getEmiAmount().multiply(BigDecimal.valueOf(12)));
        assertThat(createdLoan.getAgreementSnapshot()).isEqualTo("{\"frozen\":true}");

        verify(outboxEventWriter).write(eq("LOAN"), any(), eq("LOAN_APPROVED"), any());
    }

    @Test
    void approveThrowsWhenNotPending() {
        LoanApplication application = pendingApplication(5L);
        application.setStatus(ApplicationStatus.APPROVED);
        when(loanApplicationRepository.findById(5L)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> loanApplicationService.approve(5L)).isInstanceOf(BusinessRuleException.class);
        verify(loanApplicationRepository, never()).save(any());
    }

    @Test
    void approvePropagatesOptimisticLockConflict() {
        LoanApplication application = pendingApplication(5L);
        when(loanApplicationRepository.findById(5L)).thenReturn(Optional.of(application));
        when(loanProductRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product()));
        when(loanApplicationRepository.save(any(LoanApplication.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(LoanApplication.class, 5L));

        assertThatThrownBy(() -> loanApplicationService.approve(5L))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
        verify(loanRepository, never()).save(any());
    }

    @Test
    void rejectFlipsPendingToRejectedWithReason() {
        LoanApplication application = pendingApplication(6L);
        when(loanApplicationRepository.findById(6L)).thenReturn(Optional.of(application));
        when(loanApplicationRepository.save(any(LoanApplication.class))).thenAnswer(inv -> inv.getArgument(0));

        LoanApplicationResponse response = loanApplicationService.reject(6L, new RejectRequest("Insufficient income"));

        assertThat(response.status()).isEqualTo(ApplicationStatus.REJECTED);
        assertThat(response.rejectionReason()).isEqualTo("Insufficient income");
    }

    @Test
    void rejectThrowsWhenNotPending() {
        LoanApplication application = pendingApplication(6L);
        application.setStatus(ApplicationStatus.REJECTED);
        when(loanApplicationRepository.findById(6L)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> loanApplicationService.reject(6L, new RejectRequest("reason")))
                .isInstanceOf(BusinessRuleException.class);
        verify(loanApplicationRepository, never()).save(any());
    }

    @Test
    void getThrowsWhenMissing() {
        when(loanApplicationRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanApplicationService.get(404L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
