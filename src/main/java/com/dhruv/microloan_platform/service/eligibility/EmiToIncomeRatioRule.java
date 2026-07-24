package com.dhruv.microloan_platform.service.eligibility;

import com.dhruv.microloan_platform.entity.Borrower;
import com.dhruv.microloan_platform.entity.LoanProduct;
import com.dhruv.microloan_platform.exception.BusinessRuleException;
import com.dhruv.microloan_platform.service.EmiCalculator;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;

@Component
@Order(4)
public class EmiToIncomeRatioRule implements EligibilityRule {

    private static final BigDecimal MAX_EMI_TO_INCOME_RATIO = new BigDecimal("0.50");

    @Override
    public void check(LoanProduct product, Borrower borrower, BigDecimal requestedAmount, int requestedTenureMonths) {
        BigDecimal emi = EmiCalculator.calculateEmi(requestedAmount, product.getInterestRate(), requestedTenureMonths);
        BigDecimal ratio = emi.divide(borrower.getMonthlyIncome(), new MathContext(6));

        if (ratio.compareTo(MAX_EMI_TO_INCOME_RATIO) > 0) {
            throw new BusinessRuleException(String.format(
                    "Computed EMI %s exceeds %s%% of the borrower's monthly income %s",
                    emi, MAX_EMI_TO_INCOME_RATIO.multiply(BigDecimal.valueOf(100)), borrower.getMonthlyIncome()));
        }
    }
}
