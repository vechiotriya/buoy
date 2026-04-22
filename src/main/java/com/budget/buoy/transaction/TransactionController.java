package com.budget.buoy.transaction;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import com.budget.buoy.authentication.User;
import com.budget.buoy.authentication.UserRepository;
import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    private String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    // get current logged in user's id
    private String getCurrentUser() {
        return userRepository.findByUsername(getCurrentUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"))
                .id();
    }

    // parse date parts
    private int[] parseDateParts(String date) {
        String[] parts = date.split("-");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Date must be in format dd-MM-yyyy");
        }
        return new int[] { Integer.parseInt(parts[1]), Integer.parseInt(parts[0]) }; // month, year
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
        return transactionRepository.findByUserId(user);
    }

    @GetMapping("/transactions/{id}")
    public Transaction getTransactionById(@PathVariable Integer id) {
        return transactionRepository.findById(id).orElseThrow(TransactionNotFound::new);
    }

    // get transactions by month of an year
    @GetMapping("/transactions/month/{date}")
    public List<Transaction> getTransactionByMonth(@PathVariable String date) {
        String user = getCurrentUser();
        int[] dateParts = parseDateParts(date);
        List<Transaction> transactions = transactionRepository.findByUserId(user);
        List<Transaction> filtered = filterByMonthAndYear(transactions, dateParts[0], dateParts[1]);

        if (filtered.isEmpty())
            throw new TransactionNotFound();
        return filtered;
    }

    // get total expense and income by month of an year
    @GetMapping("/transactions/month/{date}/total")
    public Map<String, BigDecimal> getTotalByMonth(@PathVariable String date) {
        String user = getCurrentUser();
        int[] dateParts = parseDateParts(date);
        List<Transaction> transactions = transactionRepository.findByUserId(user);
        int month = dateParts[0], year = dateParts[1];
        if (transactions.isEmpty())
            throw new TransactionNotFound();

        BigDecimal totalExpense = transactions.stream()
                .filter(t -> t.transactionType() == TransactionType.Expense)
                .filter(t -> t.transactionDate().getMonthValue() == month && t.transactionDate().getYear() == year)
                .map(t -> t.amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalIncome = transactions.stream()
                .filter(t -> t.transactionType() == TransactionType.Income)
                .filter(t -> t.transactionDate().getMonthValue() == month && t.transactionDate().getYear() == year)
                .map(t -> t.amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
 
        Map<String, BigDecimal> totals = new HashMap<>();
        totals.put("totalExpense", totalExpense);
        totals.put("totalIncome", totalIncome);
        totals.put("balance", userRepository.findByUsername(getCurrentUsername()).map(User::balance).orElse(BigDecimal.ZERO));
        return totals;
    }

    // add a new transaction
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/transactions/add")
    public void addTransaction(@Valid @RequestBody Transaction transaction) {
        String userId = getCurrentUser();
        User user = userRepository.findByUsername(getCurrentUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        Transaction transactionWithEmail = new Transaction(
                NanoIdUtils.randomNanoId(new SecureRandom(), NanoIdUtils.DEFAULT_ALPHABET, 18),
                transaction.transactionType(),
                new BigDecimal(transaction.amount().toBigInteger()),
                userId,
                transaction.category(),
                transaction.purpose(),
                transaction.transactionDate(),
                transaction.version());
        transactionRepository.save(transactionWithEmail);
        BigDecimal balance = userRepository.findByUsername(getCurrentUsername()).map(User::balance).orElse(BigDecimal.ZERO);
        BigDecimal newBalance = transaction.transactionType() == TransactionType.Expense
                ? balance.subtract(transactionWithEmail.amount())
                : balance.add(transactionWithEmail.amount());
        User updatedUser = new User(user.id(), user.fullName(), user.username(), user.email(), user.password(),
                newBalance, user.version());
        userRepository.save(updatedUser);
    }

    @GetMapping("/transactions/stats/week")
    public List<GraphData> getWeekStats() {
        String user = getCurrentUser();
        Stream<Transaction> expensesByUser = transactionRepository.findByUserId(user).stream()
                .filter(t -> t.transactionType() == TransactionType.Expense);
        LocalDate today = LocalDate.now();

        WeekFields weekFields = WeekFields.of(Locale.getDefault());
        int currentWeek = today.get(weekFields.weekOfWeekBasedYear());
        int currentYear = today.getYear();

        // Group by date, summing amounts
        Map<LocalDate, BigDecimal> dailySums = expensesByUser
                .collect(Collectors.groupingBy(
                        Transaction::transactionDate,
                        Collectors.reducing(BigDecimal.ZERO, Transaction::amount, BigDecimal::add)));

        String[] labels = { "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun" };
        List<GraphData> barData = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            LocalDate day = today.with(weekFields.dayOfWeek(), i + 1); // 1 = Monday
            int week = day.get(weekFields.weekOfWeekBasedYear());
            int year = day.getYear();

            BigDecimal value = BigDecimal.ZERO;
            if (week == currentWeek && year == currentYear) {
                value = dailySums.getOrDefault(day, BigDecimal.ZERO);
            }

            if (day.equals(today)) {
                barData.add(new GraphData(value, labels[i], "#ffff")); // white or any color you want
            } else {
                barData.add(new GraphData(value, labels[i]));
            }
        }
        return barData;
    }

}
