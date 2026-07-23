package com.dhruv.microloan_platform.service.eligibility;

import com.dhruv.microloan_platform.entity.Borrower;
import com.dhruv.microloan_platform.entity.LoanProduct;
import com.dhruv.microloan_platform.exception.BusinessRuleException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Order(2)
public class TenureRangeRule implements EligibilityRule {

    @Override
    public void check(LoanProduct product, Borrower borrower, BigDecimal requestedAmount, int requestedTenureMonths) {
        if (requestedTenureMonths < product.getMinTenureMonths() || requestedTenureMonths > product.getMaxTenureMonths()) {
            throw new BusinessRuleException(String.format(
                    "Requested tenure %d months is outside the allowed range [%d, %d] for this product",
                    requestedTenureMonths, product.getMinTenureMonths(), product.getMaxTenureMonths()));
        }
    }
}
