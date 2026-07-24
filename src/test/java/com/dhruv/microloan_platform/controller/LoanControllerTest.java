package com.dhruv.microloan_platform.controller;

import com.dhruv.microloan_platform.dto.loan.InstallmentResponse;
import com.dhruv.microloan_platform.dto.loan.LoanResponse;
import com.dhruv.microloan_platform.entity.InstallmentStatus;
import com.dhruv.microloan_platform.entity.LoanStatus;
import com.dhruv.microloan_platform.exception.BusinessRuleException;
import com.dhruv.microloan_platform.exception.ResourceNotFoundException;
import com.dhruv.microloan_platform.security.CustomUserDetailsService;
import com.dhruv.microloan_platform.security.JwtAuthenticationFilter;
import com.dhruv.microloan_platform.security.JwtService;
import com.dhruv.microloan_platform.security.SecurityConfig;
import com.dhruv.microloan_platform.service.LoanService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LoanController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class LoanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LoanService loanService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private LoanResponse loanResponse(LoanStatus status) {
        return new LoanResponse(1L, 2L, 3L, new BigDecimal("100000.00"), new BigDecimal("12.00"), 12,
                new BigDecimal("8884.88"), new BigDecimal("106618.56"), BigDecimal.ZERO, status,
                "{\"principal\":100000}", null, null, Instant.now(), Instant.now());
    }

    @Test
    void getReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/loans/1")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void getReturns200WhenFound() throws Exception {
        when(loanService.get(1L)).thenReturn(loanResponse(LoanStatus.AGREEMENT_PENDING));

        mockMvc.perform(get("/loans/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AGREEMENT_PENDING"));
    }

    @Test
    @WithMockUser
    void getReturns404WhenMissing() throws Exception {
        when(loanService.get(99L)).thenThrow(new ResourceNotFoundException("Loan 99 not found"));

        mockMvc.perform(get("/loans/99")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void listReturns200WithPagedBody() throws Exception {
        when(loanService.list(any())).thenReturn(new PageImpl<>(List.of(loanResponse(LoanStatus.ACTIVE))));

        mockMvc.perform(get("/loans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"));
    }

    @Test
    @WithMockUser
    void getInstallmentsReturns200WithList() throws Exception {
        InstallmentResponse installment = new InstallmentResponse(1L, 1L, 1, LocalDate.now().plusMonths(1),
                new BigDecimal("8884.88"), BigDecimal.ZERO, new BigDecimal("8884.88"), BigDecimal.ZERO,
                InstallmentStatus.PENDING, false);
        when(loanService.getInstallments(1L)).thenReturn(List.of(installment));

        mockMvc.perform(get("/loans/1/installments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].installmentNo").value(1));
    }

    @Test
    @WithMockUser
    void acknowledgeReturns200AndActivatesLoan() throws Exception {
        when(loanService.acknowledgeAgreement(1L)).thenReturn(loanResponse(LoanStatus.ACTIVE));

        mockMvc.perform(post("/loans/1/agreement/acknowledge"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser
    void acknowledgeReturns422WhenNotAgreementPending() throws Exception {
        when(loanService.acknowledgeAgreement(1L))
                .thenThrow(new BusinessRuleException("Loan 1 is not awaiting agreement (current status: ACTIVE)"));

        mockMvc.perform(post("/loans/1/agreement/acknowledge")).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void acknowledgeReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(post("/loans/1/agreement/acknowledge")).andExpect(status().isUnauthorized());
    }
}
