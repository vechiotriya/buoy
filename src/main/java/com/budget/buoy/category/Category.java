package com.budget.buoy.category;

import java.math.BigDecimal;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

import jakarta.validation.constraints.NotNull;

@Table("categories")
public record Category(
        @Id String id,
        String userId, // FK to users table
        @NotNull String name,
        @Version Integer version) {
}
