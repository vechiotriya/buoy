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

@Component
public class BudgetSpentListener {
    private static final Logger log = LoggerFactory.getLogger(BudgetSpentListener.class);
    private final UserRepository userRepository;

    public BudgetSpentListener(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onExpenseCreated(TransactionCreatedEvent event) {
        var transaction = event.transaction();
        User currentUser = userRepository.findById(event.user().id())
            .orElseThrow(() -> new IllegalStateException("User not found"));

        // Update balance in user table
            BigDecimal balance = currentUser.balance();
            BigDecimal newBalance = transaction.transactionType() == TransactionType.Expense
                    ? balance.subtract(transaction.amount())
                    : balance.add(transaction.amount());
            User updatedUser = new User(currentUser.id(), currentUser.fullName(), currentUser.username(), currentUser.profile(), currentUser.email(),
                    currentUser.password(),
                    currentUser.provider(),
                    newBalance,null, currentUser.version());
            userRepository.save(updatedUser);
    }

}
