package com.dhruv.microloan_platform.controller;

import com.dhruv.microloan_platform.dto.loan.InstallmentResponse;
import com.dhruv.microloan_platform.dto.loan.LoanResponse;
import com.dhruv.microloan_platform.service.LoanService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** No admin restriction here - these are borrower-facing (viewing and acknowledging their own loan). */
@RestController
@RequestMapping("/loans")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @GetMapping
    public ResponseEntity<Page<LoanResponse>> list(Pageable pageable) {
        return ResponseEntity.ok(loanService.list(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoanResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(loanService.get(id));
    }

    @GetMapping("/{id}/installments")
    public ResponseEntity<List<InstallmentResponse>> getInstallments(@PathVariable Long id) {
        return ResponseEntity.ok(loanService.getInstallments(id));
    }

    @PostMapping("/{id}/agreement/acknowledge")
    public ResponseEntity<LoanResponse> acknowledgeAgreement(@PathVariable Long id) {
        return ResponseEntity.ok(loanService.acknowledgeAgreement(id));
    }
}
