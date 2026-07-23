package com.dhruv.microloan_platform.service.eligibility;

import com.dhruv.microloan_platform.entity.Borrower;
import com.dhruv.microloan_platform.entity.LoanProduct;
import com.dhruv.microloan_platform.exception.BusinessRuleException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Order(3)
public class KycLevelRule implements EligibilityRule {

    @Override
    public void check(LoanProduct product, Borrower borrower, BigDecimal requestedAmount, int requestedTenureMonths) {
        if (!borrower.getKycLevel().atLeast(product.getMinKycLevel())) {
            throw new BusinessRuleException(String.format(
                    "Borrower's KYC level %s does not meet the required minimum %s for this product",
                    borrower.getKycLevel(), product.getMinKycLevel()));
        }
    }
}
