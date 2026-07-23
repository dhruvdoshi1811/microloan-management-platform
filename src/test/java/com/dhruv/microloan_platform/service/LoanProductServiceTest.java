package com.dhruv.microloan_platform.service;

import com.dhruv.microloan_platform.dto.loanproduct.LoanProductRequest;
import com.dhruv.microloan_platform.dto.loanproduct.LoanProductResponse;
import com.dhruv.microloan_platform.entity.KycLevel;
import com.dhruv.microloan_platform.entity.LoanProduct;
import com.dhruv.microloan_platform.exception.BusinessRuleException;
import com.dhruv.microloan_platform.exception.ResourceNotFoundException;
import com.dhruv.microloan_platform.repository.LoanProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanProductServiceTest {

    @Mock
    private LoanProductRepository loanProductRepository;

    @InjectMocks
    private LoanProductService loanProductService;

    private LoanProductRequest request(String minPrincipal, String maxPrincipal, int minTenure, int maxTenure) {
        return new LoanProductRequest("Personal Loan", new BigDecimal(minPrincipal), new BigDecimal(maxPrincipal),
                minTenure, maxTenure, new BigDecimal("12.00"), new BigDecimal("2.00"), KycLevel.BASIC);
    }

    private LoanProduct existing(Long id) {
        return LoanProduct.builder()
                .id(id)
                .name("Personal Loan")
                .minPrincipal(new BigDecimal("10000"))
                .maxPrincipal(new BigDecimal("500000"))
                .minTenureMonths(6)
                .maxTenureMonths(36)
                .interestRate(new BigDecimal("12.00"))
                .penaltyRate(new BigDecimal("2.00"))
                .minKycLevel(KycLevel.BASIC)
                .build();
    }

    @Test
    void createSavesProduct() {
        when(loanProductRepository.save(any(LoanProduct.class))).thenAnswer(inv -> {
            LoanProduct saved = inv.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        LoanProductResponse response = loanProductService.create(request("10000", "500000", 6, 36));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Personal Loan");
    }

    @Test
    void createRejectsMinPrincipalGreaterThanMax() {
        assertThatThrownBy(() -> loanProductService.create(request("600000", "500000", 6, 36)))
                .isInstanceOf(BusinessRuleException.class);

        verify(loanProductRepository, never()).save(any());
    }

    @Test
    void createRejectsMinTenureGreaterThanMaxTenure() {
        assertThatThrownBy(() -> loanProductService.create(request("10000", "500000", 40, 36)))
                .isInstanceOf(BusinessRuleException.class);

        verify(loanProductRepository, never()).save(any());
    }

    @Test
    void getThrowsWhenMissing() {
        when(loanProductRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanProductService.get(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateAppliesNewFields() {
        LoanProduct existing = existing(1L);
        when(loanProductRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(loanProductRepository.save(any(LoanProduct.class))).thenAnswer(inv -> inv.getArgument(0));

        LoanProductResponse response = loanProductService.update(1L, request("20000", "600000", 12, 48));

        assertThat(response.minPrincipal()).isEqualByComparingTo("20000");
        assertThat(response.maxTenureMonths()).isEqualTo(48);
    }

    @Test
    void updateRejectsInvalidRangesBeforeTouchingRepository() {
        assertThatThrownBy(() -> loanProductService.update(1L, request("600000", "500000", 6, 36)))
                .isInstanceOf(BusinessRuleException.class);

        verify(loanProductRepository, never()).findById(any());
    }
}
