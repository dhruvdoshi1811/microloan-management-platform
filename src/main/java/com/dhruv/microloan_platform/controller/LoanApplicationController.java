package com.dhruv.microloan_platform.controller;

import com.dhruv.microloan_platform.dto.loanapplication.LoanApplicationRequest;
import com.dhruv.microloan_platform.dto.loanapplication.LoanApplicationResponse;
import com.dhruv.microloan_platform.dto.loanapplication.RejectRequest;
import com.dhruv.microloan_platform.service.LoanApplicationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/loan-applications")
public class LoanApplicationController {

    private final LoanApplicationService loanApplicationService;

    public LoanApplicationController(LoanApplicationService loanApplicationService) {
        this.loanApplicationService = loanApplicationService;
    }

    @PostMapping
    public ResponseEntity<LoanApplicationResponse> submit(@Valid @RequestBody LoanApplicationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(loanApplicationService.submit(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoanApplicationResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(loanApplicationService.get(id));
    }

    @GetMapping
    public ResponseEntity<Page<LoanApplicationResponse>> list(Pageable pageable) {
        return ResponseEntity.ok(loanApplicationService.list(pageable));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<LoanApplicationResponse> approve(@PathVariable Long id) {
        return ResponseEntity.ok(loanApplicationService.approve(id));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<LoanApplicationResponse> reject(@PathVariable Long id, @Valid @RequestBody RejectRequest request) {
        return ResponseEntity.ok(loanApplicationService.reject(id, request));
    }
}
