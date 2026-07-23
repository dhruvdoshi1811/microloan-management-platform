package com.dhruv.microloan_platform.service.eligibility;

import com.dhruv.microloan_platform.entity.Borrower;
import com.dhruv.microloan_platform.entity.LoanProduct;
import com.dhruv.microloan_platform.exception.BusinessRuleException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Order(1)
public class PrincipalRangeRule implements EligibilityRule {

    @Override
    public void check(LoanProduct product, Borrower borrower, BigDecimal requestedAmount, int requestedTenureMonths) {
        if (requestedAmount.compareTo(product.getMinPrincipal()) < 0
                || requestedAmount.compareTo(product.getMaxPrincipal()) > 0) {
            throw new BusinessRuleException(String.format(
                    "Requested amount %s is outside the allowed range [%s, %s] for this product",
                    requestedAmount, product.getMinPrincipal(), product.getMaxPrincipal()));
        }
    }
}
