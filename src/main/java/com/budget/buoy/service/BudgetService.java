package com.budget.buoy.service;

import com.budget.buoy.Application;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.budget.buoy.budget.Budget;
import com.budget.buoy.transaction.Transaction;
import com.budget.buoy.transaction.TransactionRepository;
import com.budget.buoy.transaction.TransactionType;

@Service
public class BudgetService {
    private final Application application;
    TransactionRepository transactionRepository;
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    public BudgetService(TransactionRepository transactionRepository, Application application) {
        this.transactionRepository = transactionRepository;
        this.application = application;
    }

    public BigDecimal getCurrentSpent(Budget budget) {

        LocalDateTime now = LocalDateTime.now();

        LocalDateTime start;
        LocalDateTime end;

        switch (budget.period()) {
            case Weekly -> {
                start = now.with(DayOfWeek.MONDAY)
                        .toLocalDate()
                        .atStartOfDay();

                end = start.plusWeeks(1);
            }

            case Monthly -> {
                start = now.withDayOfMonth(1)
                        .toLocalDate()
                        .atStartOfDay();

                end = start.plusMonths(1);
            }

            case Yearly -> {
                start = now.withDayOfYear(1)
                        .toLocalDate()
                        .atStartOfDay();

                end = start.plusYears(1);
            }

            default -> throw new IllegalStateException("Unknown period");
        }

        boolean isAllBudget = budget.name().equalsIgnoreCase("All");

        Stream<Transaction> transaction = transactionRepository
                .findByUserIdAndDateBetween(budget.userId(), start.toLocalDate(), end.toLocalDate())
                .stream()
                .filter(t -> t.transactionType() == TransactionType.Expense);
        if (!isAllBudget) {
            transaction = transaction.filter(t -> budgetCoversCategory(budget, t.category()));
        }
        BigDecimal sum = transaction.map(Transaction::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum;
    }

    private boolean budgetCoversCategory(Budget budget, String categoryName) {
        return budget.category() != null &&
                Arrays.asList(budget.category()).contains(categoryName);
    }
}
