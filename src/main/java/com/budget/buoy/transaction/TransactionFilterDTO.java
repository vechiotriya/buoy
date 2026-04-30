package com.budget.buoy.transaction;

import java.util.Arrays;

public class TransactionFilterDTO {
    private TransactionType type;
    private TransactionCategoryType category;
    private TransactionAmountFilterType amount;
    private TransactionDateFilterType date;

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public TransactionCategoryType getCategory() {
        return category;
    }

public void setCategory(String category) {
    if (category == null || category.trim().isEmpty()) {
        this.category = null;
        return;
    }

    String value = category.trim();

    this.category = Arrays.stream(TransactionCategoryType.values())
            .filter(c -> c.name().equalsIgnoreCase(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Invalid category: " + value));
}

    public TransactionAmountFilterType getAmount() {
        return amount;
    }

    public void setAmount(TransactionAmountFilterType amount) {
        this.amount = amount;
    }

    public TransactionDateFilterType getDate() {
        return date;
    }

    public void setDate(TransactionDateFilterType date) {
        this.date = date;
    }
}
