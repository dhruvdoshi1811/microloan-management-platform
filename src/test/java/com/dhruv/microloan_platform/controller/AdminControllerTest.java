package com.dhruv.microloan_platform.controller;

import com.dhruv.microloan_platform.dto.outbox.OutboxEventResponse;
import com.dhruv.microloan_platform.dto.outbox.OverdueCheckResult;
import com.dhruv.microloan_platform.entity.OutboxEventStatus;
import com.dhruv.microloan_platform.security.CustomUserDetailsService;
import com.dhruv.microloan_platform.security.JwtAuthenticationFilter;
import com.dhruv.microloan_platform.security.JwtService;
import com.dhruv.microloan_platform.security.SecurityConfig;
import com.dhruv.microloan_platform.service.OutboxEventService;
import com.dhruv.microloan_platform.service.OverdueService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** See AuthControllerTest for why SecurityConfig + JwtAuthenticationFilter are imported and mocked here. */
@WebMvcTest(AdminController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OverdueService overdueService;
    @MockitoBean
    private OutboxEventService outboxEventService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void runOverdueCheckReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(post("/admin/run-overdue-check")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "BORROWER")
    void runOverdueCheckReturns403ForNonAdmin() throws Exception {
        mockMvc.perform(post("/admin/run-overdue-check")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void runOverdueCheckReturns200ForAdmin() throws Exception {
        when(overdueService.runOverdueCheck()).thenReturn(new OverdueCheckResult(5, 2, 3, 3));

        mockMvc.perform(post("/admin/run-overdue-check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loansScanned").value(5))
                .andExpect(jsonPath("$.loansMarkedOverdue").value(2));
    }

    @Test
    void getOutboxReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/admin/outbox")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "BORROWER")
    void getOutboxReturns403ForNonAdmin() throws Exception {
        mockMvc.perform(get("/admin/outbox")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getOutboxReturns200WithFilteredResultsForAdmin() throws Exception {
        OutboxEventResponse event = new OutboxEventResponse(1L, "LOAN", 10L, "LOAN_APPROVED",
                "{}", OutboxEventStatus.PENDING, Instant.now(), null);
        when(outboxEventService.list(eq(OutboxEventStatus.PENDING), any())).thenReturn(new PageImpl<>(List.of(event)));

        mockMvc.perform(get("/admin/outbox").param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].eventType").value("LOAN_APPROVED"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getOutboxReturns200WithAllResultsWhenStatusOmitted() throws Exception {
        when(outboxEventService.list(isNull(), any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/admin/outbox")).andExpect(status().isOk());
    }
}
