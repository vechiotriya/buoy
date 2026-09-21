package com.budget.buoy.category;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import com.budget.buoy.authentication.UserRepository;
import com.budget.buoy.transaction.Transaction;
import com.budget.buoy.transaction.TransactionRepository;
import com.budget.buoy.transaction.TransactionType;

import jakarta.validation.Valid;

@RestController
public class CategoryController {

        private final CategoryRepository categoryRepository;
        private final UserRepository userRepository;
        private final TransactionRepository transactionRepository;

        record TransactionCategoryGroup(BigDecimal value, String text) {
        }

        // get current logged in user's id
        private String getCurrentUser() {
                String username = SecurityContextHolder.getContext().getAuthentication().getName();
                return userRepository.findByUsername(username)
                                .orElseThrow(() -> new UsernameNotFoundException("User not found"))
                                .id();
        }

        public CategoryController(CategoryRepository categoryRepository, UserRepository userRepository,
                        TransactionRepository transactionRepository) {
                this.categoryRepository = categoryRepository;
                this.userRepository = userRepository;
                this.transactionRepository = transactionRepository;
        }

        // Get all categories for the current user
        @GetMapping("/categories")
        public List<Category> getAllCategories() {
                String userId = getCurrentUser();
                return categoryRepository.findByUserId(userId);
        }

        // Add a new category for the current user
        @ResponseStatus(HttpStatus.CREATED)
        @PostMapping("/categories/add")
        public ResponseEntity<?> addCategory(@Valid @RequestBody Category category) {
                String userId = getCurrentUser();
                if (categoryRepository.existsByNameAndUserId(category.name(), userId)) {
                        return ResponseEntity.badRequest()
                                        .body(Map.of("error", "Category with this name already exists for the user"));
                }
                Category newCategory = new Category(
                                NanoIdUtils.randomNanoId(new SecureRandom(), NanoIdUtils.DEFAULT_ALPHABET, 18),
                                userId,
                                category.name(),
                                category.version());
                categoryRepository.save(newCategory);
                return ResponseEntity.ok().body(Map.of("message", "Category added for: " + newCategory.userId()));
        }

        @GetMapping("/categories/transactions")
        public ResponseEntity<?> getExpensePercentByCategory() {
                String userId = getCurrentUser();
                WeekFields weekFields = WeekFields.ISO;
                LocalDate today = LocalDate.now();
                int currentWeek = today.get(weekFields.weekOfWeekBasedYear());
                int currentWeekYear = today.get(weekFields.weekBasedYear());

                // Single DB fetch
                List<Transaction> allExpenses = transactionRepository.findByUserId(userId)
                                .stream()
                                .filter(t -> t.transactionType() == TransactionType.Expense)
                                .toList();

                List<Transaction> weeklyExpenses = allExpenses.stream()
                                .filter(t -> {
                                        LocalDate d = t.transaction_date();
                                        return d.get(weekFields.weekOfWeekBasedYear()) == currentWeek
                                                        && d.get(weekFields.weekBasedYear()) == currentWeekYear;
                                })
                                .toList();

                List<Transaction> yearlyExpenses = allExpenses.stream()
                                .filter(t -> t.transaction_date().getYear() == today.getYear())
                                .toList();

                Map<String, List<TransactionCategoryGroup>> result = new HashMap<>();
                result.put("week", percentByCategory(weeklyExpenses));
                result.put("year", percentByCategory(yearlyExpenses));
                return ResponseEntity.ok(result);
        }

        private static final String UNCATEGORIZED = "Uncategorized";

        private List<TransactionCategoryGroup> percentByCategory(List<Transaction> expenses) {
                BigDecimal grandTotal = expenses.stream()
                                .map(Transaction::amount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                if (grandTotal.compareTo(BigDecimal.ZERO) == 0) {
                        return List.of();
                }

                Map<String, BigDecimal> totalsByCategory = expenses.stream()
                                .collect(Collectors.groupingBy(
                                                t -> t.category() == null ? UNCATEGORIZED : t.category().toString(),
                                                Collectors.reducing(BigDecimal.ZERO, Transaction::amount,
                                                                BigDecimal::add)));

                return totalsByCategory.entrySet().stream()
                                .map(e -> new TransactionCategoryGroup(
                                                e.getValue()
                                                                .divide(grandTotal, 4, RoundingMode.HALF_UP)
                                                                .multiply(BigDecimal.valueOf(100)),
                                                e.getKey()))
                                .toList();
        }
}
