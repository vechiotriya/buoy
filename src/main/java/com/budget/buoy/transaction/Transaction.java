package com.budget.buoy.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;

public record Transaction(
        @Id String id,
        TransactionType transactionType,
        @Column BigDecimal amount,
        String user_id, // FK to users table
        String category,
        String purpose,
        LocalDate transaction_date,
        @CreatedDate @Column("created_at") LocalDateTime createdAt,
        @Version Integer version) {
    public Transaction {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }
}
