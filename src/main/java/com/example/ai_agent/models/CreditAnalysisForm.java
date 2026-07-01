package com.example.ai_agent.models;

import java.math.BigDecimal;

public record CreditAnalysisForm(
        BigDecimal totalAmount,
        Integer termMonths,
        BigDecimal monthlyIncome
) {
    public boolean isComplete() {
        return totalAmount != null && termMonths != null && monthlyIncome != null;
    }

    public CreditAnalysisForm withTotalAmount(BigDecimal value) {
        return new CreditAnalysisForm(value, termMonths, monthlyIncome);
    }

    public CreditAnalysisForm withTermMonths(Integer value) {
        return new CreditAnalysisForm(totalAmount, value, monthlyIncome);
    }

    public CreditAnalysisForm withMonthlyIncome(BigDecimal value) {
        return new CreditAnalysisForm(totalAmount, termMonths, value);
    }

    public static CreditAnalysisForm empty() {
        return new CreditAnalysisForm(null, null, null);
    }
}
