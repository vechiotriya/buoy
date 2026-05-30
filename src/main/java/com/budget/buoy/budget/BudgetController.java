package com.budget.buoy.budget;

import org.springframework.web.bind.annotation.RestController;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import com.budget.buoy.authentication.UserRepository;
import com.budget.buoy.category.CategoryRepository;
import com.budget.buoy.service.BudgetService;
import com.budget.buoy.service.UserService;

import jakarta.validation.Valid;

import java.security.SecureRandom;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;

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
    public void addBudget(@Valid @RequestBody Budget budget) {
        Budget newBudget = new Budget(NanoIdUtils.randomNanoId(new SecureRandom(), NanoIdUtils.DEFAULT_ALPHABET, 18),
                getCurrentUser(), budget.amount(), budget.period(), budget.category(),null, budget.name(),
                budget.version());
        budgetRepository.save(newBudget);
    }

    @ResponseStatus(HttpStatus.OK)
    @DeleteMapping("/budget/delete")
    public void deleteBudget(@RequestBody String id) {
        budgetRepository.deleteById(id);
    }

    @GetMapping("/budget/all")
    public List<BudgetResponse> getAllBudgets() {
        String userId = getCurrentUser();
        List<Budget> budgets = budgetRepository.findByUserId(userId);
        return budgets.stream().map(budget -> new BudgetResponse(budget.id(), budget.name(), budget.amount(), budgetService.getCurrentSpent(budget), budget.period(), budget.category(), budget.createdAt())).toList();
    }

}
