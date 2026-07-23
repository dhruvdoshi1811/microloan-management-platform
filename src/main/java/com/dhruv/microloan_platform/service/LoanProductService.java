package com.dhruv.microloan_platform.service;

import com.dhruv.microloan_platform.dto.loanproduct.LoanProductRequest;
import com.dhruv.microloan_platform.dto.loanproduct.LoanProductResponse;
import com.dhruv.microloan_platform.entity.LoanProduct;
import com.dhruv.microloan_platform.exception.BusinessRuleException;
import com.dhruv.microloan_platform.exception.ResourceNotFoundException;
import com.dhruv.microloan_platform.repository.LoanProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoanProductService {

    private final LoanProductRepository loanProductRepository;

    public LoanProductService(LoanProductRepository loanProductRepository) {
        this.loanProductRepository = loanProductRepository;
    }

    @Transactional
    public LoanProductResponse create(LoanProductRequest request) {
        validateRanges(request);

        LoanProduct product = LoanProduct.builder()
                .name(request.name())
                .minPrincipal(request.minPrincipal())
                .maxPrincipal(request.maxPrincipal())
                .minTenureMonths(request.minTenureMonths())
                .maxTenureMonths(request.maxTenureMonths())
                .interestRate(request.interestRate())
                .penaltyRate(request.penaltyRate())
                .minKycLevel(request.minKycLevel())
                .build();

        return LoanProductResponse.from(loanProductRepository.save(product));
    }

    public LoanProductResponse get(Long id) {
        return LoanProductResponse.from(findOrThrow(id));
    }

    public Page<LoanProductResponse> list(Pageable pageable) {
        return loanProductRepository.findAll(pageable).map(LoanProductResponse::from);
    }

    @Transactional
    public LoanProductResponse update(Long id, LoanProductRequest request) {
        validateRanges(request);
        LoanProduct product = findOrThrow(id);

        product.setName(request.name());
        product.setMinPrincipal(request.minPrincipal());
        product.setMaxPrincipal(request.maxPrincipal());
        product.setMinTenureMonths(request.minTenureMonths());
        product.setMaxTenureMonths(request.maxTenureMonths());
        product.setInterestRate(request.interestRate());
        product.setPenaltyRate(request.penaltyRate());
        product.setMinKycLevel(request.minKycLevel());

        return LoanProductResponse.from(loanProductRepository.save(product));
    }

    private void validateRanges(LoanProductRequest request) {
        if (request.minPrincipal().compareTo(request.maxPrincipal()) > 0) {
            throw new BusinessRuleException("Minimum principal cannot exceed maximum principal");
        }
        if (request.minTenureMonths() > request.maxTenureMonths()) {
            throw new BusinessRuleException("Minimum tenure cannot exceed maximum tenure");
        }
    }

    private LoanProduct findOrThrow(Long id) {
        return loanProductRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan product " + id + " not found"));
    }
}
