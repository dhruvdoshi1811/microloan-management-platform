package com.dhruv.microloan_platform.service;

import com.dhruv.microloan_platform.dto.loanapplication.LoanApplicationRequest;
import com.dhruv.microloan_platform.dto.loanapplication.LoanApplicationResponse;
import com.dhruv.microloan_platform.dto.loanapplication.RejectRequest;
import com.dhruv.microloan_platform.entity.ApplicationStatus;
import com.dhruv.microloan_platform.entity.Borrower;
import com.dhruv.microloan_platform.entity.LoanApplication;
import com.dhruv.microloan_platform.entity.LoanProduct;
import com.dhruv.microloan_platform.exception.BusinessRuleException;
import com.dhruv.microloan_platform.exception.ResourceNotFoundException;
import com.dhruv.microloan_platform.repository.BorrowerRepository;
import com.dhruv.microloan_platform.repository.LoanApplicationRepository;
import com.dhruv.microloan_platform.repository.LoanProductRepository;
import com.dhruv.microloan_platform.service.eligibility.LoanEligibilityService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fetches Borrower/LoanProduct via their own repositories directly (same cross-repository
 * pattern KycService uses) rather than calling BorrowerService/LoanProductService - services
 * talk to repositories, not to each other, in this codebase.
 */
@Service
public class LoanApplicationService {

    private final LoanApplicationRepository loanApplicationRepository;
    private final BorrowerRepository borrowerRepository;
    private final LoanProductRepository loanProductRepository;
    private final LoanEligibilityService loanEligibilityService;

    public LoanApplicationService(LoanApplicationRepository loanApplicationRepository,
                                   BorrowerRepository borrowerRepository,
                                   LoanProductRepository loanProductRepository,
                                   LoanEligibilityService loanEligibilityService) {
        this.loanApplicationRepository = loanApplicationRepository;
        this.borrowerRepository = borrowerRepository;
        this.loanProductRepository = loanProductRepository;
        this.loanEligibilityService = loanEligibilityService;
    }

    @Transactional
    public LoanApplicationResponse submit(LoanApplicationRequest request) {
        Borrower borrower = borrowerRepository.findById(request.borrowerId())
                .orElseThrow(() -> new ResourceNotFoundException("Borrower " + request.borrowerId() + " not found"));
        LoanProduct product = loanProductRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Loan product " + request.productId() + " not found"));

        loanEligibilityService.checkEligibility(product, borrower, request.requestedAmount(), request.requestedTenureMonths());

        LoanApplication application = LoanApplication.builder()
                .borrowerId(borrower.getId())
                .productId(product.getId())
                .requestedAmount(request.requestedAmount())
                .requestedTenureMonths(request.requestedTenureMonths())
                .build();

        return LoanApplicationResponse.from(loanApplicationRepository.save(application));
    }

    public LoanApplicationResponse get(Long id) {
        return LoanApplicationResponse.from(findOrThrow(id));
    }

    public Page<LoanApplicationResponse> list(Pageable pageable) {
        return loanApplicationRepository.findAll(pageable).map(LoanApplicationResponse::from);
    }

    @Transactional
    public LoanApplicationResponse approve(Long id) {
        LoanApplication application = findOrThrow(id);
        requirePending(application);
        application.setStatus(ApplicationStatus.APPROVED);
        return LoanApplicationResponse.from(loanApplicationRepository.save(application));
    }

    @Transactional
    public LoanApplicationResponse reject(Long id, RejectRequest request) {
        LoanApplication application = findOrThrow(id);
        requirePending(application);
        application.setStatus(ApplicationStatus.REJECTED);
        application.setRejectionReason(request.rejectionReason());
        return LoanApplicationResponse.from(loanApplicationRepository.save(application));
    }

    private void requirePending(LoanApplication application) {
        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new BusinessRuleException("Loan application " + application.getId() + " is not pending (current status: "
                    + application.getStatus() + ")");
        }
    }

    private LoanApplication findOrThrow(Long id) {
        return loanApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan application " + id + " not found"));
    }
}
