package com.dhruv.microloan_platform.service.eligibility;

import com.dhruv.microloan_platform.entity.Borrower;
import com.dhruv.microloan_platform.entity.LoanProduct;
import com.dhruv.microloan_platform.exception.BusinessRuleException;

import java.math.BigDecimal;

public interface EligibilityRule {

    void check(LoanProduct product, Borrower borrower, BigDecimal requestedAmount, int requestedTenureMonths);
}
