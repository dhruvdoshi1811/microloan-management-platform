package com.dhruv.microloan_platform.controller;

import com.dhruv.microloan_platform.dto.loanproduct.LoanProductRequest;
import com.dhruv.microloan_platform.dto.loanproduct.LoanProductResponse;
import com.dhruv.microloan_platform.entity.KycLevel;
import com.dhruv.microloan_platform.exception.ResourceNotFoundException;
import com.dhruv.microloan_platform.security.CustomUserDetailsService;
import com.dhruv.microloan_platform.security.JwtAuthenticationFilter;
import com.dhruv.microloan_platform.security.JwtService;
import com.dhruv.microloan_platform.security.SecurityConfig;
import com.dhruv.microloan_platform.service.LoanProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LoanProductController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class LoanProductControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LoanProductService loanProductService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private LoanProductRequest validRequest() {
        return new LoanProductRequest("Personal Loan", new BigDecimal("10000"), new BigDecimal("500000"),
                6, 36, new BigDecimal("12.00"), new BigDecimal("2.00"), KycLevel.BASIC);
    }

    private LoanProductResponse response() {
        return new LoanProductResponse(1L, "Personal Loan", new BigDecimal("10000"), new BigDecimal("500000"),
                6, 36, new BigDecimal("12.00"), new BigDecimal("2.00"), KycLevel.BASIC, true,
                Instant.now(), Instant.now());
    }

    @Test
    void createReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(post("/loan-products")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "BORROWER")
    void createReturns403ForNonAdmin() throws Exception {
        mockMvc.perform(post("/loan-products")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createReturns201ForAdmin() throws Exception {
        when(loanProductService.create(any(LoanProductRequest.class))).thenReturn(response());

        mockMvc.perform(post("/loan-products")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Personal Loan"));
    }

    @Test
    @WithMockUser(roles = "BORROWER")
    void getReturns200ForAnyAuthenticatedRole() throws Exception {
        when(loanProductService.get(1L)).thenReturn(response());

        mockMvc.perform(get("/loan-products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser
    void getReturns404WhenMissing() throws Exception {
        when(loanProductService.get(99L)).thenThrow(new ResourceNotFoundException("Loan product 99 not found"));

        mockMvc.perform(get("/loan-products/99")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateReturns200ForAdmin() throws Exception {
        when(loanProductService.update(eq(1L), any(LoanProductRequest.class))).thenReturn(response());

        mockMvc.perform(put("/loan-products/1")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "BORROWER")
    void updateReturns403ForNonAdmin() throws Exception {
        mockMvc.perform(put("/loan-products/1")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void listReturns200WithPagedBody() throws Exception {
        when(loanProductService.list(any())).thenReturn(new PageImpl<>(List.of(response())));

        mockMvc.perform(get("/loan-products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Personal Loan"));
    }
}
