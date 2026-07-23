package com.dhruv.microloan_platform.controller;

import com.dhruv.microloan_platform.dto.loanapplication.LoanApplicationRequest;
import com.dhruv.microloan_platform.dto.loanapplication.LoanApplicationResponse;
import com.dhruv.microloan_platform.dto.loanapplication.RejectRequest;
import com.dhruv.microloan_platform.entity.ApplicationStatus;
import com.dhruv.microloan_platform.exception.BusinessRuleException;
import com.dhruv.microloan_platform.security.CustomUserDetailsService;
import com.dhruv.microloan_platform.security.JwtAuthenticationFilter;
import com.dhruv.microloan_platform.security.JwtService;
import com.dhruv.microloan_platform.security.SecurityConfig;
import com.dhruv.microloan_platform.service.LoanApplicationService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LoanApplicationController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class LoanApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LoanApplicationService loanApplicationService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private LoanApplicationRequest validRequest() {
        return new LoanApplicationRequest(1L, 2L, new BigDecimal("100000"), 12);
    }

    private LoanApplicationResponse response(ApplicationStatus status) {
        return new LoanApplicationResponse(1L, 1L, 2L, new BigDecimal("100000"), 12, status, null,
                Instant.now(), Instant.now());
    }

    @Test
    void submitReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(post("/loan-applications")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "BORROWER")
    void submitReturns201ForAnyAuthenticatedRole() throws Exception {
        when(loanApplicationService.submit(any(LoanApplicationRequest.class))).thenReturn(response(ApplicationStatus.PENDING));

        mockMvc.perform(post("/loan-applications")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @WithMockUser
    void getReturns200() throws Exception {
        when(loanApplicationService.get(1L)).thenReturn(response(ApplicationStatus.PENDING));

        mockMvc.perform(get("/loan-applications/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void approveReturns200ForAdmin() throws Exception {
        when(loanApplicationService.approve(1L)).thenReturn(response(ApplicationStatus.APPROVED));

        mockMvc.perform(post("/loan-applications/1/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @WithMockUser(roles = "BORROWER")
    void approveReturns403ForNonAdmin() throws Exception {
        mockMvc.perform(post("/loan-applications/1/approve")).andExpect(status().isForbidden());
    }

    @Test
    void approveReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(post("/loan-applications/1/approve")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void rejectReturns200ForAdmin() throws Exception {
        RejectRequest request = new RejectRequest("Insufficient income");
        when(loanApplicationService.reject(eq(1L), any(RejectRequest.class))).thenReturn(response(ApplicationStatus.REJECTED));

        mockMvc.perform(post("/loan-applications/1/reject")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    @WithMockUser(roles = "BORROWER")
    void rejectReturns403ForNonAdmin() throws Exception {
        RejectRequest request = new RejectRequest("Insufficient income");

        mockMvc.perform(post("/loan-applications/1/reject")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void approveReturns422WhenNotPending() throws Exception {
        when(loanApplicationService.approve(1L))
                .thenThrow(new BusinessRuleException("Loan application 1 is not pending (current status: APPROVED)"));

        mockMvc.perform(post("/loan-applications/1/approve")).andExpect(status().isUnprocessableEntity());
    }
}
