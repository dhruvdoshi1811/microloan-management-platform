package com.dhruv.microloan_platform.service.eligibility;

import com.dhruv.microloan_platform.entity.Borrower;
import com.dhruv.microloan_platform.entity.LoanProduct;
import com.dhruv.microloan_platform.exception.BusinessRuleException;

import java.math.BigDecimal;

/**
 * One eligibility check. Implementations throw {@link BusinessRuleException} on violation;
 * {@link LoanEligibilityService} runs every implementation in order and stops at the first
 * failure. Adding a new rule means adding a new implementation, not editing existing ones.
 */
public interface EligibilityRule {

    void check(LoanProduct product, Borrower borrower, BigDecimal requestedAmount, int requestedTenureMonths);
}
