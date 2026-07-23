package com.dhruv.microloan_platform.controller;

import com.dhruv.microloan_platform.dto.loanproduct.LoanProductRequest;
import com.dhruv.microloan_platform.dto.loanproduct.LoanProductResponse;
import com.dhruv.microloan_platform.service.LoanProductService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Write endpoints (POST/PUT) are restricted to ADMIN in SecurityConfig; reads are open to any authenticated user. */
@RestController
@RequestMapping("/loan-products")
public class LoanProductController {

    private final LoanProductService loanProductService;

    public LoanProductController(LoanProductService loanProductService) {
        this.loanProductService = loanProductService;
    }

    @PostMapping
    public ResponseEntity<LoanProductResponse> create(@Valid @RequestBody LoanProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(loanProductService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoanProductResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(loanProductService.get(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LoanProductResponse> update(@PathVariable Long id, @Valid @RequestBody LoanProductRequest request) {
        return ResponseEntity.ok(loanProductService.update(id, request));
    }

    @GetMapping
    public ResponseEntity<Page<LoanProductResponse>> list(Pageable pageable) {
        return ResponseEntity.ok(loanProductService.list(pageable));
    }
}
