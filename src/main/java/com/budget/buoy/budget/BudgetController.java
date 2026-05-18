package com.budget.buoy.budget;

import org.springframework.web.bind.annotation.RestController;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import com.budget.buoy.authentication.UserRepository;
import com.budget.buoy.category.CategoryRepository;

import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;

import org.springframework.boot.autoconfigure.security.SecurityProperties.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
public class BudgetController {
    BudgetRepository budgetRepository;
    CategoryRepository categoryRepository;
    UserRepository userRepository;

    public BudgetController(BudgetRepository budgetRepository, CategoryRepository categoryRepository,
            UserRepository userRepository) {
        this.budgetRepository = budgetRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
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
    public void addBudget(@Valid @RequestBody Budget budget) {
        Budget newBudget = new Budget(NanoIdUtils.randomNanoId(new SecureRandom(), NanoIdUtils.DEFAULT_ALPHABET, 18),
                getCurrentUser(), budget.amount(), BigDecimal.ZERO, budget.period(), budget.category(), budget.name(),
                budget.version());
        budgetRepository.save(newBudget);
    }

    @GetMapping("/budget/all")
    public List<Budget> getAllBudgets() {
        String userId = getCurrentUser();
        List<Budget> budgets = budgetRepository.findByUserId(userId);
        return budgets;
    }

}
