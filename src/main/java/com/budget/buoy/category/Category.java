package com.budget.buoy.category;

import java.math.BigDecimal;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

import jakarta.validation.constraints.NotNull;

@Table("categories")
public record Category(
        @Id Integer id,
        String userId, // FK to users table
        @NotNull String name,
        @NotNull BigDecimal budget,
        @Version Integer version) {
    public Category {
        if (budget.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Budget must be greater than zero");
        }
        
    }
}
