package com.dhruv.microloan_platform.controller;

import com.dhruv.microloan_platform.dto.repayment.RepaymentRequest;
import com.dhruv.microloan_platform.dto.repayment.RepaymentResponse;
import com.dhruv.microloan_platform.exception.BusinessRuleException;
import com.dhruv.microloan_platform.exception.ResourceNotFoundException;
import com.dhruv.microloan_platform.security.CustomUserDetailsService;
import com.dhruv.microloan_platform.security.JwtAuthenticationFilter;
import com.dhruv.microloan_platform.security.JwtService;
import com.dhruv.microloan_platform.security.SecurityConfig;
import com.dhruv.microloan_platform.service.RepaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** See AuthControllerTest for why SecurityConfig + JwtAuthenticationFilter are imported and mocked here. */
@WebMvcTest(RepaymentController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class RepaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RepaymentService repaymentService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private RepaymentRequest validRequest() {
        return new RepaymentRequest(1L, new BigDecimal("20000.00"), "REF-1", "UPI");
    }

    private RepaymentResponse response() {
        return new RepaymentResponse(1L, 1L, new BigDecimal("20000.00"), "REF-1", "UPI",
                new BigDecimal("180000.00"), Instant.now());
    }

    @Test
    void processReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(post("/repayments")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void processReturns201ForValidRequest() throws Exception {
        when(repaymentService.processRepayment(any(RepaymentRequest.class))).thenReturn(response());

        mockMvc.perform(post("/repayments")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentReference").value("REF-1"));
    }

    @Test
    @WithMockUser
    void processReturns400WhenAmountMissing() throws Exception {
        String body = """
                {"loanId": 1, "paymentReference": "REF-1", "paymentMode": "UPI"}
                """;

        mockMvc.perform(post("/repayments").contentType("application/json").content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void processReturns422WhenAmountExceedsOutstanding() throws Exception {
        when(repaymentService.processRepayment(any(RepaymentRequest.class)))
                .thenThrow(new BusinessRuleException("Repayment amount exceeds outstanding balance"));

        mockMvc.perform(post("/repayments")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @WithMockUser
    void getReturns200WhenFound() throws Exception {
        when(repaymentService.get(1L)).thenReturn(response());

        mockMvc.perform(get("/repayments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(20000.00));
    }

    @Test
    @WithMockUser
    void getReturns404WhenMissing() throws Exception {
        when(repaymentService.get(99L)).thenThrow(new ResourceNotFoundException("Repayment 99 not found"));

        mockMvc.perform(get("/repayments/99")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void getByLoanReturns200WithList() throws Exception {
        when(repaymentService.getByLoan(1L)).thenReturn(List.of(response()));

        mockMvc.perform(get("/loans/1/repayments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].paymentReference").value("REF-1"));
    }

    @Test
    @WithMockUser
    void getByLoanReturns404WhenLoanMissing() throws Exception {
        when(repaymentService.getByLoan(99L)).thenThrow(new ResourceNotFoundException("Loan 99 not found"));

        mockMvc.perform(get("/loans/99/repayments")).andExpect(status().isNotFound());
    }
}
