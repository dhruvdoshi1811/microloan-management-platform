package com.dhruv.microloan_platform.controller;

import com.dhruv.microloan_platform.dto.auth.AuthResponse;
import com.dhruv.microloan_platform.dto.auth.LoginRequest;
import com.dhruv.microloan_platform.dto.auth.RegisterRequest;
import com.dhruv.microloan_platform.dto.auth.UserResponse;
import com.dhruv.microloan_platform.entity.Role;
import com.dhruv.microloan_platform.security.CustomUserDetailsService;
import com.dhruv.microloan_platform.security.JwtAuthenticationFilter;
import com.dhruv.microloan_platform.security.JwtService;
import com.dhruv.microloan_platform.security.SecurityConfig;
import com.dhruv.microloan_platform.service.AuthService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void registerReturns201ForValidRequest() throws Exception {
        RegisterRequest request = new RegisterRequest("alice@example.com", "password123", Role.BORROWER);
        when(authService.register(request))
                .thenReturn(new UserResponse(1L, "alice@example.com", Role.BORROWER, Instant.now()));

        mockMvc.perform(post("/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.role").value("BORROWER"));
    }

    @Test
    void registerReturns400ForInvalidEmail() throws Exception {
        String body = """
                {"email": "not-an-email", "password": "password123", "role": "BORROWER"}
                """;

        mockMvc.perform(post("/auth/register").contentType("application/json").content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    void loginReturns200WithToken() throws Exception {
        LoginRequest request = new LoginRequest("alice@example.com", "password123");
        when(authService.login(request))
                .thenReturn(AuthResponse.bearer("signed-jwt", 3_600_000));

        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("signed-jwt"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void loginReturns400WhenPasswordMissing() throws Exception {
        String body = """
                {"email": "alice@example.com"}
                """;

        mockMvc.perform(post("/auth/login").contentType("application/json").content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void meReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "alice@example.com")
    void meReturns200WhenAuthenticated() throws Exception {
        when(authService.me("alice@example.com"))
                .thenReturn(new UserResponse(1L, "alice@example.com", Role.BORROWER, Instant.now()));

        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@example.com"));
    }
}
