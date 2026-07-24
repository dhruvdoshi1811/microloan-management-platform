package com.dhruv.microloan_platform.service.eligibility;

import com.dhruv.microloan_platform.entity.Borrower;
import com.dhruv.microloan_platform.entity.LoanProduct;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class LoanEligibilityService {

    private final List<EligibilityRule> rules;

    public LoanEligibilityService(List<EligibilityRule> rules) {
        this.rules = rules;
    }

    public void checkEligibility(LoanProduct product, Borrower borrower, BigDecimal requestedAmount, int requestedTenureMonths) {
        rules.forEach(rule -> rule.check(product, borrower, requestedAmount, requestedTenureMonths));
    }
}
