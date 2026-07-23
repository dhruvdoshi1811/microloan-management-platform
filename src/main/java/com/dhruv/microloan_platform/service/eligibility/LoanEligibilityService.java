package com.dhruv.microloan_platform.service.eligibility;

import com.dhruv.microloan_platform.entity.Borrower;
import com.dhruv.microloan_platform.entity.LoanProduct;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * The "engine": runs every {@link EligibilityRule} bean, in the order Spring injects them
 * (governed by each rule's {@code @Order}), stopping at the first one that throws. Adding a
 * new rule never requires touching this class.
 */
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
