package com.budget.buoy.budget;

import com.budget.buoy.authentication.User;
import com.budget.buoy.authentication.UserRepository;
import com.budget.buoy.transaction.TransactionCreatedEvent;
import com.budget.buoy.transaction.TransactionType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Component
public class BudgetSpentListener {
    private static final Logger log = LoggerFactory.getLogger(BudgetSpentListener.class);
    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;

    public BudgetSpentListener(BudgetRepository budgetRepository, UserRepository userRepository) {
        this.budgetRepository = budgetRepository;
        this.userRepository = userRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onExpenseCreated(TransactionCreatedEvent event) {
        var transaction = event.transaction();
        User currentUser = userRepository.findById(event.user().id())
            .orElseThrow(() -> new IllegalStateException("User not found"));
        // Update affected budgets
        List<Budget> matchingBudgets = budgetRepository
                .findByUserId(transaction.user_id())
                .stream()
                .filter(budget -> budgetCoversCategory(budget, transaction.category().name())|| "All".equals(budget.name()))
                .toList();

        if (matchingBudgets.isEmpty()) {
            log.warn("No matching budget found for user={} category={}",
                    transaction.user_id(), transaction.category());
            return;
        }

        matchingBudgets.forEach(budget -> {
            BigDecimal updatedSpent = budget.spent().add(transaction.amount());
            Budget updated = new Budget(
                    budget.id(),
                    budget.userId(),
                    budget.amount(),
                    updatedSpent,
                    budget.period(),
                    budget.category(),
                    budget.name(),
                    budget.version());

            budgetRepository.save(updated);
            log.info("Budget '{}' spent updated: {} -> {}", budget.name(), budget.spent(), updatedSpent);
        });

        // Update balance in user table
            BigDecimal balance = currentUser.balance();
            BigDecimal newBalance = transaction.transactionType() == TransactionType.Expense
                    ? balance.subtract(transaction.amount())
                    : balance.add(transaction.amount());
            User updatedUser = new User(currentUser.id(), currentUser.fullName(), currentUser.username(), currentUser.profile(), currentUser.email(),
                    currentUser.password(),
                    currentUser.provider(),
                    newBalance, currentUser.version());
            userRepository.save(updatedUser);
    }

    private boolean budgetCoversCategory(Budget budget, String categoryName) {
        return budget.category() != null &&
                Arrays.asList(budget.category()).contains(categoryName);
    }
}
