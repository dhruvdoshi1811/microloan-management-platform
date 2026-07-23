package com.dhruv.microloan_platform.service;

import com.dhruv.microloan_platform.dto.borrower.BorrowerRequest;
import com.dhruv.microloan_platform.dto.borrower.BorrowerResponse;
import com.dhruv.microloan_platform.entity.Borrower;
import com.dhruv.microloan_platform.exception.DuplicateResourceException;
import com.dhruv.microloan_platform.exception.ResourceNotFoundException;
import com.dhruv.microloan_platform.repository.BorrowerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BorrowerService {

    private final BorrowerRepository borrowerRepository;

    public BorrowerService(BorrowerRepository borrowerRepository) {
        this.borrowerRepository = borrowerRepository;
    }

    @Transactional
    public BorrowerResponse create(BorrowerRequest request) {
        if (borrowerRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("A borrower with email " + request.email() + " already exists");
        }

        Borrower borrower = Borrower.builder()
                .fullName(request.fullName())
                .phone(request.phone())
                .email(request.email())
                .dob(request.dob())
                .monthlyIncome(request.monthlyIncome())
                .build();

        return BorrowerResponse.from(borrowerRepository.save(borrower));
    }

    public BorrowerResponse get(Long id) {
        return BorrowerResponse.from(findOrThrow(id));
    }

    public Page<BorrowerResponse> list(Pageable pageable) {
        return borrowerRepository.findAll(pageable).map(BorrowerResponse::from);
    }

    @Transactional
    public BorrowerResponse update(Long id, BorrowerRequest request) {
        Borrower borrower = findOrThrow(id);

        boolean emailChanged = !borrower.getEmail().equals(request.email());
        if (emailChanged && borrowerRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("A borrower with email " + request.email() + " already exists");
        }

        borrower.setFullName(request.fullName());
        borrower.setPhone(request.phone());
        borrower.setEmail(request.email());
        borrower.setDob(request.dob());
        borrower.setMonthlyIncome(request.monthlyIncome());

        return BorrowerResponse.from(borrowerRepository.save(borrower));
    }

    private Borrower findOrThrow(Long id) {
        return borrowerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Borrower " + id + " not found"));
    }
}
