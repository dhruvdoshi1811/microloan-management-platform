package com.dhruv.microloan_platform.controller;

import com.dhruv.microloan_platform.dto.repayment.RepaymentRequest;
import com.dhruv.microloan_platform.dto.repayment.RepaymentResponse;
import com.dhruv.microloan_platform.service.RepaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * No class-level @RequestMapping, unlike every other controller so far - these three paths
 * (/repayments, /repayments/{id}, /loans/{id}/repayments) don't share a common prefix.
 * No admin restriction; not marked so in the endpoint map.
 */
@RestController
public class RepaymentController {

    private final RepaymentService repaymentService;

    public RepaymentController(RepaymentService repaymentService) {
        this.repaymentService = repaymentService;
    }

    @PostMapping("/repayments")
    public ResponseEntity<RepaymentResponse> process(@Valid @RequestBody RepaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(repaymentService.processRepayment(request));
    }

    @GetMapping("/repayments/{id}")
    public ResponseEntity<RepaymentResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(repaymentService.get(id));
    }

    @GetMapping("/loans/{id}/repayments")
    public ResponseEntity<List<RepaymentResponse>> getByLoan(@PathVariable Long id) {
        return ResponseEntity.ok(repaymentService.getByLoan(id));
    }
}
