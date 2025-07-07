package com.budget.buoy.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

import jakarta.annotation.Generated;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;



public record Transaction(
    @Id
    Integer id,
    TransactionType transactionType,
    BigDecimal amount,
    String userId,  // FK to users table
    String category,
    String purpose,
    String transactionSource,
    LocalDate transactionDate,
    @Version
    Integer version
) {
    public Transaction {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }
}
