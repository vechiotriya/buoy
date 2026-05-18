package com.budget.buoy.budget;

import java.math.BigDecimal;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;
import jakarta.validation.constraints.NotNull;

@Table("budget")
public record Budget(
    @Id String id,
    String userId, // FK to users table
    @NotNull BigDecimal amount,
    @NotNull BigDecimal spent,
    @NotNull BudgetPeriodType period,
    String[] category,
    @NotNull String name,
    @Version Integer version) {
    public Budget{
        if(amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }
}
 