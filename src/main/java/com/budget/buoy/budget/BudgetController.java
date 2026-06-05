package com.budget.buoy.budget;

import org.springframework.web.bind.annotation.RestController;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import com.budget.buoy.authentication.BudgetingStyle;
import com.budget.buoy.authentication.User;
import com.budget.buoy.authentication.UserRepository;
import com.budget.buoy.category.CategoryRepository;
import com.budget.buoy.service.BudgetService;
import jakarta.validation.Valid;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
public class BudgetController {
    BudgetRepository budgetRepository;
    CategoryRepository categoryRepository;
    UserRepository userRepository;
    BudgetService budgetService;
    private static final Logger logger = LoggerFactory.getLogger(BudgetController.class);

    public BudgetController(BudgetRepository budgetRepository, CategoryRepository categoryRepository,
            UserRepository userRepository, BudgetService budgetService) {
        this.budgetRepository = budgetRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.budgetService = budgetService;
    }

    private String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    // get current logged in user's id
    private String getCurrentUser() {
        return userRepository.findByUsername(getCurrentUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"))
                .id();
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/budget/add")
    public ResponseEntity<?> addBudget(@Valid @RequestBody Budget budget) {
        User user = userRepository.findByUsername(getCurrentUsername()).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        List<Budget> budgets = budgetRepository.findByUserId(user.id());
        if (user.prefBudgetStyle().equals(BudgetingStyle.Standard) && budgets.stream().anyMatch(b -> b.period().equals(budget.period()))) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message","Budget for this period already exists for the user"));
        }
        Budget newBudget = new Budget(NanoIdUtils.randomNanoId(new SecureRandom(), NanoIdUtils.DEFAULT_ALPHABET, 18),
                getCurrentUser(), budget.amount(), budget.period(), budget.category(), null, budget.name(),
                budget.version());
        budgetRepository.save(newBudget);
        return ResponseEntity.ok().body(Map.of("message", "Budget added"));
    }

    @DeleteMapping("/budget/delete/{id}")
    public void deleteBudget(@PathVariable String id) {
        logger.info("Deleting budget: {}", id);
        budgetRepository.deleteById(id);
    }

    @GetMapping("/budget/all")
    public List<BudgetResponse> getAllBudgets() {
        String userId = getCurrentUser();
        List<Budget> budgets = budgetRepository.findByUserId(userId);
        return budgets.stream()
                .map(budget -> new BudgetResponse(budget.id(), budget.name(), budget.amount(),
                        budgetService.getCurrentSpent(budget), budget.period(), budget.category(), budget.createdAt()))
                .toList();
    }

}
