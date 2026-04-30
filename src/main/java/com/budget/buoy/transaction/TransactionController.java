package com.budget.buoy.transaction;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import com.budget.buoy.authentication.User;
import com.budget.buoy.authentication.UserRepository;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
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

    record TransactionGroup(String month, String year, BigDecimal total, List<Transaction> transactions) {
    }

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

    // Get the first date and last date of a month by a given date
    private LocalDate[] getMonthEndAndStart(String date) {
        String[] parts = date.split("-");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Date must be in format dd-MM-yyyy");
        }
        YearMonth yearMonth = YearMonth.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();
        return new LocalDate[] { start, end };
    }

    @GetMapping("/transactions")
    public List<Transaction> getAllTransactions() {
        String user = getCurrentUser();
        return transactionRepository.findByUserId(user);
    }

    @GetMapping("/transactions/grouped")
    public List<TransactionGroup> getGroupedTransactions(TransactionFilterDTO filter) {
        String user = getCurrentUser();
        List<Transaction> transactions = transactionRepository.findByUserId(user);
        TransactionType type = filter.getType();
        TransactionCategoryType category = filter.getCategory();
        TransactionAmountFilterType amount = filter.getAmount();
        TransactionDateFilterType date = filter.getDate();
        Stream<Transaction> stream = transactions.stream();
        if (amount != null) {
            if (amount.equals(TransactionAmountFilterType.Upto200)) {
                stream = stream.filter(t -> t.amount().compareTo(BigDecimal.valueOf(200)) <= 0);
            } else if (amount.equals(TransactionAmountFilterType.From200to500)) {
                stream = stream.filter(t -> t.amount().compareTo(BigDecimal.valueOf(200)) >= 0
                        && t.amount().compareTo(BigDecimal.valueOf(500)) <= 0);
            } else if (amount.equals(TransactionAmountFilterType.From500to2000)) {
                stream = stream.filter(t -> t.amount().compareTo(BigDecimal.valueOf(500)) >= 0
                        && t.amount().compareTo(BigDecimal.valueOf(2000)) <= 0);
            } else if (amount.equals(TransactionAmountFilterType.Above2000)) {
                stream = stream.filter(t -> t.amount().compareTo(BigDecimal.valueOf(2000)) > 0);
            }
        }

        if (date != null) {
            if (date.equals(TransactionDateFilterType.Today)) {
                stream = stream.filter(t -> t.transaction_date().isEqual(LocalDate.now()));
            } else if (date.equals(TransactionDateFilterType.Thisweek)) {
                LocalDate thisMonday = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
                stream = stream.filter(t -> !t.transaction_date().isBefore(thisMonday));
            } else if (date.equals(TransactionDateFilterType.Thismonth)) {
                LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
                stream = stream.filter(t -> !t.transaction_date().isBefore(startOfMonth));
            } else if (date.equals(TransactionDateFilterType.Last3months)) {
                LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
                stream = stream.filter(t -> !t.transaction_date().isBefore(startOfMonth.minusMonths(2)));
            }
        }

        return stream.filter(t -> type == null || t.transactionType() == type)
                .filter(t -> category == null || java.util.Objects.equals(t.category(), category))
                .collect(Collectors.groupingBy(
                        t -> t.transaction_date().getMonth().toString() + "-" + t.transaction_date().getYear())) /// month-year
                .entrySet().stream().map(entry -> {
                    List<Transaction> group = entry.getValue(); // get the list of transactions of that group
                    BigDecimal total = group.stream() // sum the amount of the transactions
                            .map(Transaction::amount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    Transaction first = group.get(0); // get the first transaction just in order to get the month and
                                                      // year
                    return new TransactionGroup(
                            first.transaction_date().getMonth().toString(),
                            String.valueOf(first.transaction_date().getYear()),
                            total,
                            group);
                }).sorted(Comparator
                        .comparingInt((TransactionGroup g) -> Integer.parseInt(g.year()))
                        .thenComparingInt((TransactionGroup g) -> Month.valueOf(g.month()).getValue()))
                .toList();
    }

    @GetMapping("/transactions/search/{param}")
    public List<TransactionGroup> searchTransaction(@PathVariable String param) {
        String user = getCurrentUser();
        List<Transaction> transactions = transactionRepository.findByUserId(user);
        return transactions.stream().filter(t -> t.purpose().contains(param)
                || t.amount().toString().contains(param))
                .collect(Collectors.groupingBy(
                        t -> t.transaction_date().getMonth().toString() + "-" + t.transaction_date().getYear())) /// month-year
                .entrySet().stream().map(entry -> {
                    List<Transaction> group = entry.getValue(); // get the list of transactions of that group
                    BigDecimal total = group.stream() // sum the amount of the transactions
                            .map(Transaction::amount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    Transaction first = group.get(0); // get the first transaction just in order to get the month and
                                                      // year
                    return new TransactionGroup(
                            first.transaction_date().getMonth().toString(),
                            String.valueOf(first.transaction_date().getYear()),
                            total,
                            group);
                }).sorted(Comparator
                        .comparingInt((TransactionGroup g) -> Integer.parseInt(g.year()))
                        .thenComparingInt((TransactionGroup g) -> Month.valueOf(g.month()).getValue()))
                .toList();
    }

    @GetMapping("/transactions/{id}")
    public Transaction getTransactionById(@PathVariable Integer id) {
        return transactionRepository.findById(id).orElseThrow(TransactionNotFound::new);
    }

    // get transactions by month of an year
    @GetMapping("/transactions/month/{date}")
    public List<Transaction> getTransactionByMonth(@PathVariable String date) {
        String user = getCurrentUser();
        LocalDate[] range = getMonthEndAndStart(date);
        LocalDate start = range[0];
        LocalDate end = range[1];
        List<Transaction> transactions = transactionRepository.findByUserIdAndDateBetween(user,
                start, end);
        if (transactions.isEmpty())
            throw new TransactionNotFound();
        return transactions;
    }

    // get a month's total stats(balance, total expense, total income)
    @GetMapping("/transactions/month/{date}/total")
    public Map<String, BigDecimal> getTotalByMonth(@PathVariable String date) {
        String user = getCurrentUser();
        LocalDate[] range = getMonthEndAndStart(date);
        LocalDate start = range[0];
        LocalDate end = range[1];
        List<Transaction> transactions = transactionRepository.findByUserIdAndDateBetween(user,
                start, end);
        if (transactions.isEmpty())
            throw new TransactionNotFound();

        BigDecimal totalExpense = transactions.stream()
                .filter(t -> t.transactionType() == TransactionType.Expense)
                .map(t -> t.amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalIncome = transactions.stream()
                .filter(t -> t.transactionType() == TransactionType.Income)
                .map(t -> t.amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> totals = new HashMap<>();
        totals.put("totalExpense", totalExpense);
        totals.put("totalIncome", totalIncome);
        totals.put("balance",
                userRepository.findByUsername(getCurrentUsername()).map(User::balance).orElse(BigDecimal.ZERO));
        return totals;
    }

    // add a new transaction
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/transactions/add")
    public void addTransaction(@Valid @RequestBody Transaction transaction) {
        User user = userRepository.findByUsername(getCurrentUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        Transaction transactionWithEmail = new Transaction(
                NanoIdUtils.randomNanoId(new SecureRandom(), NanoIdUtils.DEFAULT_ALPHABET, 18),
                transaction.transactionType(),
                transaction.amount(),
                user.id(),
                transaction.category(),
                transaction.purpose(),
                transaction.transaction_date(),
                transaction.version());
        transactionRepository.save(transactionWithEmail);
        BigDecimal balance = user.balance();
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

        WeekFields weekFields = WeekFields.ISO;
        int currentWeek = today.get(weekFields.weekOfWeekBasedYear());
        int currentYear = today.getYear();

        // Group by date, summing amounts
        Map<LocalDate, BigDecimal> dailySums = expensesByUser
                .collect(Collectors.groupingBy(
                        Transaction::transaction_date,
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
