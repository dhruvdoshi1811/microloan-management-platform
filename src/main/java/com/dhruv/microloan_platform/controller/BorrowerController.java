package com.dhruv.microloan_platform.controller;

import com.dhruv.microloan_platform.dto.borrower.BorrowerRequest;
import com.dhruv.microloan_platform.dto.borrower.BorrowerResponse;
import com.dhruv.microloan_platform.service.BorrowerService;
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

@RestController
@RequestMapping("/borrowers")
public class BorrowerController {

    private final BorrowerService borrowerService;

    public BorrowerController(BorrowerService borrowerService) {
        this.borrowerService = borrowerService;
    }

    @PostMapping
    public ResponseEntity<BorrowerResponse> create(@Valid @RequestBody BorrowerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(borrowerService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BorrowerResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(borrowerService.get(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BorrowerResponse> update(@PathVariable Long id, @Valid @RequestBody BorrowerRequest request) {
        return ResponseEntity.ok(borrowerService.update(id, request));
    }

    @GetMapping
    public ResponseEntity<Page<BorrowerResponse>> list(Pageable pageable) {
        return ResponseEntity.ok(borrowerService.list(pageable));
    }
}
