package com.dhruv.microloan_platform.controller;

import com.dhruv.microloan_platform.dto.outbox.OutboxEventResponse;
import com.dhruv.microloan_platform.dto.outbox.OverdueCheckResult;
import com.dhruv.microloan_platform.entity.OutboxEventStatus;
import com.dhruv.microloan_platform.service.OutboxEventService;
import com.dhruv.microloan_platform.service.OverdueService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final OverdueService overdueService;
    private final OutboxEventService outboxEventService;

    public AdminController(OverdueService overdueService, OutboxEventService outboxEventService) {
        this.overdueService = overdueService;
        this.outboxEventService = outboxEventService;
    }

    @PostMapping("/run-overdue-check")
    public ResponseEntity<OverdueCheckResult> runOverdueCheck() {
        return ResponseEntity.ok(overdueService.runOverdueCheck());
    }

    @GetMapping("/outbox")
    public ResponseEntity<Page<OutboxEventResponse>> getOutboxEvents(
            @RequestParam(required = false) OutboxEventStatus status, Pageable pageable) {
        return ResponseEntity.ok(outboxEventService.list(status, pageable));
    }
}
