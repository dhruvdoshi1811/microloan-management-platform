package com.dhruv.microloan_platform.controller;

import com.dhruv.microloan_platform.dto.kyc.KycInitiateRequest;
import com.dhruv.microloan_platform.dto.kyc.KycResponse;
import com.dhruv.microloan_platform.dto.kyc.OtpInitiateResponse;
import com.dhruv.microloan_platform.dto.kyc.OtpVerifyRequest;
import com.dhruv.microloan_platform.entity.DocumentType;
import com.dhruv.microloan_platform.entity.KycLevel;
import com.dhruv.microloan_platform.exception.BusinessRuleException;
import com.dhruv.microloan_platform.exception.ResourceNotFoundException;
import com.dhruv.microloan_platform.security.CustomUserDetailsService;
import com.dhruv.microloan_platform.security.JwtAuthenticationFilter;
import com.dhruv.microloan_platform.security.JwtService;
import com.dhruv.microloan_platform.security.SecurityConfig;
import com.dhruv.microloan_platform.service.KycService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(KycController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class KycControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private KycService kycService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void initiateReturns401WhenUnauthenticated() throws Exception {
        KycInitiateRequest request = new KycInitiateRequest(DocumentType.PAN, "ABCDE1234F");

        mockMvc.perform(post("/borrowers/1/kyc/initiate")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void initiateReturns201WithOtp() throws Exception {
        KycInitiateRequest request = new KycInitiateRequest(DocumentType.PAN, "ABCDE1234F");
        when(kycService.initiate(eq(1L), any(KycInitiateRequest.class)))
                .thenReturn(new OtpInitiateResponse(1L, DocumentType.PAN, "123456", Instant.now().plusSeconds(300)));

        mockMvc.perform(post("/borrowers/1/kyc/initiate")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.otpCode").value("123456"));
    }

    @Test
    @WithMockUser
    void initiateReturns404WhenBorrowerMissing() throws Exception {
        KycInitiateRequest request = new KycInitiateRequest(DocumentType.PAN, "ABCDE1234F");
        when(kycService.initiate(eq(99L), any(KycInitiateRequest.class)))
                .thenThrow(new ResourceNotFoundException("Borrower 99 not found"));

        mockMvc.perform(post("/borrowers/99/kyc/initiate")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void verifyOtpReturns200OnSuccess() throws Exception {
        OtpVerifyRequest request = new OtpVerifyRequest(DocumentType.PAN, "123456");
        when(kycService.verifyOtp(eq(1L), any(OtpVerifyRequest.class)))
                .thenReturn(new KycResponse(1L, "ABCDE1234F", null, true, false, KycLevel.BASIC));

        mockMvc.perform(post("/borrowers/1/kyc/verify-otp")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.panVerified").value(true))
                .andExpect(jsonPath("$.kycLevel").value("BASIC"));
    }

    @Test
    @WithMockUser
    void verifyOtpReturns422ForWrongCode() throws Exception {
        OtpVerifyRequest request = new OtpVerifyRequest(DocumentType.PAN, "000000");
        when(kycService.verifyOtp(eq(1L), any(OtpVerifyRequest.class)))
                .thenThrow(new BusinessRuleException("Invalid OTP code"));

        mockMvc.perform(post("/borrowers/1/kyc/verify-otp")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Invalid OTP code"));
    }

    @Test
    @WithMockUser
    void getKycReturns200WhenPresent() throws Exception {
        when(kycService.getKyc(1L)).thenReturn(new KycResponse(1L, "ABCDE1234F", null, true, false, KycLevel.BASIC));

        mockMvc.perform(get("/borrowers/1/kyc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kycLevel").value("BASIC"));
    }

    @Test
    @WithMockUser
    void getKycReturns404WhenNotInitiated() throws Exception {
        when(kycService.getKyc(1L)).thenThrow(new ResourceNotFoundException("KYC has not been initiated for borrower 1"));

        mockMvc.perform(get("/borrowers/1/kyc"))
                .andExpect(status().isNotFound());
    }
}
