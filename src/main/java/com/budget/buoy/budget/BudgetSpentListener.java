package com.budget.buoy.budget;

import com.budget.buoy.authentication.User;
import com.budget.buoy.authentication.UserRepository;
import com.budget.buoy.service.BudgetService;
import com.budget.buoy.service.FCMService;
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
import java.util.List;

@Component
public class BudgetSpentListener {
    private static final Logger log = LoggerFactory.getLogger(BudgetSpentListener.class);
    private final UserRepository userRepository;
    private BudgetRepository budgetRepository;
    private FCMService fcmService;
    BudgetService budgetService;

    public BudgetSpentListener(UserRepository userRepository, BudgetRepository budgetRepository, FCMService fcmService, BudgetService budgetService) {
        this.userRepository = userRepository;
        this.budgetRepository = budgetRepository;
        this.fcmService = fcmService;
        this.budgetService = budgetService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onExpenseCreated(TransactionCreatedEvent event) {
        try {
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
                    newBalance,currentUser.prefBudgetStyle(),currentUser.fcmToken(), currentUser.version());
            userRepository.save(updatedUser);

            List<Budget> budgets = budgetRepository.findByUserId(currentUser.id());
            for (Budget budget : budgets) {
                BigDecimal totalSpent=budgetService.getCurrentSpent(budget);
                int spentPercentage=totalSpent.divide(budget.amount()).multiply(BigDecimal.valueOf(100)).intValue();
                if (spentPercentage >=60 && spentPercentage < 80) {
                    log.info("{} % of Budget spent {}",spentPercentage, budget.name());
                    fcmService.sendNotification(updatedUser.fcmToken(), "Budget Alert⚠️", "You have spent more than half of your budget for " + budget.name()+", You better watch out👀");
                }
                else if(spentPercentage>=80 && spentPercentage<100){
                    log.info("{} % of Budget spent {}",spentPercentage, budget.name());
                    fcmService.sendNotification(updatedUser.fcmToken(), "Budget Alert🚨", "You have spent "+spentPercentage+"% of your budget for " + budget.name()+", Stop spending already😒");
                }
            }

        } catch (Exception e) {
            log.info("Budget listener failed with : {}",e);
            throw new RuntimeException(e);
        }

}
}
