package com.budget.buoy.transaction;

import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

import jakarta.validation.constraints.Email;



public record Transaction(
    @Id
    Integer id,
    TransactionType transactionType,
    Float amount,
    @Email
    String email,
    String category,
    String purpose,
    String transactionSource,
    LocalDate transactionDate,
    @Version
    Integer version
) {
    public Transaction {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }
}
