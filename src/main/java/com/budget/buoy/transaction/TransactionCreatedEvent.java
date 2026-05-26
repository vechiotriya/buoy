package com.budget.buoy.transaction;

import com.budget.buoy.authentication.User;

public record TransactionCreatedEvent(Transaction transaction,User user) {}