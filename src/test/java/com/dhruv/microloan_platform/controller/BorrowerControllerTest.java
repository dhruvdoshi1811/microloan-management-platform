package com.dhruv.microloan_platform.controller;

import com.dhruv.microloan_platform.dto.borrower.BorrowerRequest;
import com.dhruv.microloan_platform.dto.borrower.BorrowerResponse;
import com.dhruv.microloan_platform.entity.KycLevel;
import com.dhruv.microloan_platform.exception.ResourceNotFoundException;
import com.dhruv.microloan_platform.security.CustomUserDetailsService;
import com.dhruv.microloan_platform.security.JwtAuthenticationFilter;
import com.dhruv.microloan_platform.security.JwtService;
import com.dhruv.microloan_platform.security.SecurityConfig;
import com.dhruv.microloan_platform.service.BorrowerService;
import tools.jackson.databind.ObjectMapper;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * See AuthControllerTest for why SecurityConfig + JwtAuthenticationFilter are imported and
 * JwtService/CustomUserDetailsService are mocked rather than exercised for real here.
 */
@WebMvcTest(BorrowerController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class BorrowerControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BorrowerService borrowerService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private BorrowerRequest validRequest() {
        return new BorrowerRequest("Alice Borrower", "9999999999", "alice@example.com",
                LocalDate.of(1995, 1, 1), new BigDecimal("50000.00"));
    }

    private BorrowerResponse response() {
        return new BorrowerResponse(1L, "Alice Borrower", "9999999999", "alice@example.com",
                LocalDate.of(1995, 1, 1), new BigDecimal("50000.00"), KycLevel.NONE, true,
                Instant.now(), Instant.now());
    }

    @Test
    void createReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(post("/borrowers")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void createReturns201ForValidRequest() throws Exception {
        when(borrowerService.create(any(BorrowerRequest.class))).thenReturn(response());

        mockMvc.perform(post("/borrowers")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.kycLevel").value("NONE"));
    }

    @Test
    @WithMockUser
    void createReturns400WhenFullNameBlank() throws Exception {
        String body = """
                {"fullName": "", "phone": "9999999999", "email": "alice@example.com",
                 "dob": "1995-01-01", "monthlyIncome": 50000.00}
                """;

        mockMvc.perform(post("/borrowers").contentType("application/json").content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.fullName").exists());
    }

    @Test
    @WithMockUser
    void getReturns200WhenFound() throws Exception {
        when(borrowerService.get(1L)).thenReturn(response());

        mockMvc.perform(get("/borrowers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser
    void getReturns404WhenMissing() throws Exception {
        when(borrowerService.get(99L)).thenThrow(new ResourceNotFoundException("Borrower 99 not found"));

        mockMvc.perform(get("/borrowers/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Borrower 99 not found"));
    }

    @Test
    @WithMockUser
    void updateReturns200ForValidRequest() throws Exception {
        when(borrowerService.update(eq(1L), any(BorrowerRequest.class))).thenReturn(response());

        mockMvc.perform(put("/borrowers/1")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@example.com"));
    }

    @Test
    @WithMockUser
    void listReturns200WithPagedBody() throws Exception {
        when(borrowerService.list(any())).thenReturn(new PageImpl<>(List.of(response())));

        mockMvc.perform(get("/borrowers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("alice@example.com"));
    }
}
