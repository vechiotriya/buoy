package com.budget.buoy.budget;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BudgetResponse(
        String id,
        String name,
        BigDecimal amount,
        BigDecimal spent,
        BudgetPeriodType period,
        String[] category,
        LocalDateTime createdAt
) {
    public BudgetResponse(String id, String name, BigDecimal amount, BigDecimal spent, BudgetPeriodType period, String[] category, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.amount = amount;
        this.spent = spent;
        this.period = period;
        this.category = category;
        this.createdAt = createdAt;
    }
}
