package com.budget.buoy.transaction;

import org.springframework.web.bind.annotation.RestController;
import com.budget.buoy.authentication.User;
import com.budget.buoy.authentication.UserRepository;
import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@RestController
public class TransactionController {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TransactionController.class);

    public TransactionController(TransactionRepository transactionRepository, UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    // get current logged in user's id
    private String getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"))
                .id();
    }

    // parse date parts
    private int[] parseDateParts(String date) {
        String[] parts = date.split("-");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Date must be in format dd-MM-yyyy");
        }
        return new int[]{Integer.parseInt(parts[1]), Integer.parseInt(parts[2])}; // month, year
    }

    // filter by month & year
    private List<Transaction> filterByMonthAndYear(List<Transaction> transactions, int month, int year) {
        return transactions.stream()
                .filter(t -> t.transactionDate().getMonthValue() == month && t.transactionDate().getYear() == year)
                .toList();
    }

    @GetMapping("/transactions")
    public List<Transaction> getAllTransactions() {
        String user = getCurrentUser();
        logger.info("Current user: {}", user);
        return transactionRepository.findByUserId(user);
    }

    @GetMapping("/transactions/{id}")
    public Transaction getTransactionById(@PathVariable Integer id) {
        return transactionRepository.findById(id).orElseThrow(TransactionNotFound::new);
    }

    //get transactions by month of an year
    @GetMapping("/transactions/month/{date}")
    public List<Transaction> getTransactionByMonth(@PathVariable String date) {
        String user = getCurrentUser();
        int[] dateParts = parseDateParts(date);
        List<Transaction> transactions = transactionRepository.findByUserId(user);
        List<Transaction> filtered = filterByMonthAndYear(transactions, dateParts[0], dateParts[1]);

        if (filtered.isEmpty()) throw new TransactionNotFound();
        return filtered;
    }

    // get total expense and income by month of an year
    @GetMapping("/transactions/month/{date}/total")
    public Map<String, Double> getTotalByMonth(@PathVariable String date) {
        String user = getCurrentUser();
        int[] dateParts = parseDateParts(date);
        List<Transaction> transactions = transactionRepository.findByUserId(user);
        int month = dateParts[0], year = dateParts[1];

        if (transactions.isEmpty()) throw new TransactionNotFound();

        double totalExpense = transactions.stream()
                .filter(t -> t.transactionType() == TransactionType.Expense)
                .filter(t -> t.transactionDate().getMonthValue() == month && t.transactionDate().getYear() == year)
                .mapToDouble(t -> t.amount().doubleValue())
                .sum();

        double totalIncome = transactions.stream()
                .filter(t -> t.transactionType() == TransactionType.Income)
                .filter(t -> t.transactionDate().getMonthValue() == month && t.transactionDate().getYear() == year)
                .mapToDouble(t -> t.amount().doubleValue())
                .sum();

        Map<String, Double> totals = new HashMap<>();
        totals.put("totalExpense", totalExpense);
        totals.put("totalIncome", totalIncome);
        return totals;
    }

    // add a new transaction
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/transactions/add")
    public void addTransaction(@Valid @RequestBody Transaction transaction) {
        String user = getCurrentUser();
        Transaction transactionWithEmail = new Transaction(
                transaction.id(),
                transaction.transactionType(),
                new BigDecimal(transaction.amount().toBigInteger()),
                user,
                transaction.category(),
                transaction.purpose(),
                transaction.transactionSource(),
                transaction.transactionDate(),
                transaction.version()
        );
        transactionRepository.save(transactionWithEmail);
    }
}
