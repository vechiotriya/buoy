package com.budget.buoy.transaction;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import com.budget.buoy.authentication.User;
import com.budget.buoy.authentication.UserRepository;
import com.budget.buoy.service.UserService;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@RestController
public class TransactionController {

        private final UserRepository userRepository;
        private final TransactionRepository transactionRepository;
        private static final Logger logger = LoggerFactory.getLogger(UserService.class);

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
                return transactionRepository.findByUserId(user)
                                .stream()
                                .sorted(Comparator.comparing(Transaction::transaction_date).reversed())
                                .toList();
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
                                stream = stream.filter(
                                                t -> !t.transaction_date().isBefore(startOfMonth.minusMonths(2)));
                        }
                }

                return stream.filter(t -> type == null || t.transactionType() == type)
                                .filter(t -> category == null || java.util.Objects.equals(t.category(), category))
                                .collect(Collectors.groupingBy(
                                                t -> t.transaction_date().getMonth().toString() + "-"
                                                                + t.transaction_date().getYear())) /// month-year
                                .entrySet().stream().map(entry -> {
                                        List<Transaction> group = entry.getValue(); // get the list of transactions of
                                                                                    // that group
                                        BigDecimal total = group.stream() // sum the amount of the transactions
                                                        .map(Transaction::amount)
                                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                                        Transaction first = group.get(0); // get the first transaction just in order to
                                                                          // get the month and
                                                                          // year
                                        return new TransactionGroup(
                                                        first.transaction_date().getMonth().toString(),
                                                        String.valueOf(first.transaction_date().getYear()),
                                                        total,
                                                        group);
                                }).sorted(Comparator
                                                .comparingInt((TransactionGroup g) -> Integer.parseInt(g.year()))
                                                .thenComparingInt((TransactionGroup g) -> Month.valueOf(g.month())
                                                                .getValue())
                                                .reversed())
                                .toList();
        }

        @GetMapping("/transactions/search/{param}")
        public List<TransactionGroup> searchTransaction(@PathVariable String param) {
                String user = getCurrentUser();
                List<Transaction> transactions = transactionRepository.findByUserId(user);
                return transactions.stream().filter(t -> t.purpose().contains(param)
                                || t.amount().toString().contains(param))
                                .collect(Collectors.groupingBy(
                                                t -> t.transaction_date().getMonth().toString() + "-"
                                                                + t.transaction_date().getYear())) /// month-year
                                .entrySet().stream().map(entry -> {
                                        List<Transaction> group = entry.getValue(); // get the list of transactions of
                                                                                    // that group
                                        BigDecimal total = group.stream() // sum the amount of the transactions
                                                        .map(Transaction::amount)
                                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                                        Transaction first = group.get(0); // get the first transaction just in order to
                                                                          // get the month and
                                                                          // year
                                        return new TransactionGroup(
                                                        first.transaction_date().getMonth().toString(),
                                                        String.valueOf(first.transaction_date().getYear()),
                                                        total,
                                                        group);
                                }).sorted(Comparator
                                                .comparingInt((TransactionGroup g) -> Integer.parseInt(g.year()))
                                                .thenComparingInt((TransactionGroup g) -> Month.valueOf(g.month())
                                                                .getValue())
                                                .reversed())
                                .toList();
        }

        @GetMapping("/transactions/{id}")
        public Transaction getTransactionById(@PathVariable String id) {
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
                                userRepository.findByUsername(getCurrentUsername()).map(User::balance)
                                                .orElse(BigDecimal.ZERO));
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
                                user.provider(),
                                newBalance, user.version());
                userRepository.save(updatedUser);
        }

        @GetMapping("/transactions/stats/week")
        public ResponseEntity<StatsData> getWeekStats() {
                String user = getCurrentUser();
                LocalDate today = LocalDate.now();
                WeekFields weekFields = WeekFields.ISO;
                int currentWeek = today.get(weekFields.weekOfWeekBasedYear());
                int currentYear = today.getYear();

                List<Transaction> expenses = transactionRepository.findByUserId(user).stream()
                                .filter(t -> t.transactionType() == TransactionType.Expense)
                                .collect(Collectors.toList());

                // --- Graph Data (filtered to current week) ---
                Map<LocalDate, BigDecimal> dailySums = expenses.stream()
                                .filter(t -> {
                                        int week = t.transaction_date().get(weekFields.weekOfWeekBasedYear());
                                        int year = t.transaction_date().getYear();
                                        return week == currentWeek && year == currentYear;
                                })
                                .collect(Collectors.groupingBy(
                                                Transaction::transaction_date,
                                                Collectors.reducing(BigDecimal.ZERO, Transaction::amount,
                                                                BigDecimal::add)));

                String[] labels = { "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun" };
                List<GraphData> barData = new ArrayList<>();
                logger.info("aha {}", dailySums);
                for (int i = 0; i < 7; i++) {
                        LocalDate day = today.with(weekFields.dayOfWeek(), i + 1);
                        BigDecimal value = dailySums.getOrDefault(day, BigDecimal.ZERO);

                        barData.add(day.equals(today)
                                        ? new GraphData(value, labels[i], "#ffff")
                                        : new GraphData(value, labels[i]));
                }

                // --- Stats ---
                BigDecimal total = dailySums.values().stream()
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Top spending day (from current week only)
                Map.Entry<LocalDate, BigDecimal> topDay = dailySums.entrySet().stream()
                                .max(Map.Entry.comparingByValue())
                                .orElse(null);

                String topSpending = topDay != null
                                ? topDay.getKey().getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                                : "N/A";
                BigDecimal topSpendingAmount = topDay != null ? topDay.getValue() : BigDecimal.ZERO;

                // Change since last week (%)
                int lastWeek = currentWeek - 1;
                int lastWeekYear = currentWeek == 1 ? currentYear - 1 : currentYear;
                BigDecimal lastWeekTotal = expenses.stream()
                                .filter(t -> {
                                        int week = t.transaction_date().get(weekFields.weekOfWeekBasedYear());
                                        int year = t.transaction_date().getYear();
                                        return week == lastWeek && year == lastWeekYear;
                                })
                                .map(Transaction::amount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                Integer changeSinceLast = lastWeekTotal.compareTo(BigDecimal.ZERO) == 0 ? 0
                                : total.subtract(lastWeekTotal)
                                                .divide(lastWeekTotal, 2, RoundingMode.HALF_UP)
                                                .multiply(BigDecimal.valueOf(100))
                                                .intValue();

                return ResponseEntity
                                .ok(new StatsData(total, changeSinceLast, topSpending, topSpendingAmount, barData));
        }

        @GetMapping("/transactions/stats/lastWeek")
        public ResponseEntity<?> getLastWeekStats() {
                String user = getCurrentUser();
                LocalDate today = LocalDate.now();
                LocalDate lastWeekAnchor = today.minusWeeks(1);
                WeekFields weekFields = WeekFields.ISO;

                int lastWeek = lastWeekAnchor.get(weekFields.weekOfWeekBasedYear());
                int lastWeekYear = lastWeekAnchor.getYear();

                List<Transaction> expenses = transactionRepository.findByUserId(user).stream()
                                .filter(t -> t.transactionType() == TransactionType.Expense)
                                .collect(Collectors.toList());

                Map<LocalDate, BigDecimal> dailySums = expenses.stream()
                                .filter(t -> {
                                        int week = t.transaction_date().get(weekFields.weekOfWeekBasedYear());
                                        int year = t.transaction_date().getYear();
                                        return week == lastWeek && year == lastWeekYear; 
                                })
                                .collect(Collectors.groupingBy(
                                                Transaction::transaction_date,
                                                Collectors.reducing(BigDecimal.ZERO, Transaction::amount,
                                                                BigDecimal::add)));

                logger.info("dailySums for last week: {}", dailySums);

                String[] labels = { "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun" };
                List<GraphData> barData = new ArrayList<>();

                for (int i = 0; i < 7; i++) {
                        LocalDate day = lastWeekAnchor.with(weekFields.dayOfWeek(), i + 1);
                        BigDecimal value = dailySums.getOrDefault(day, BigDecimal.ZERO);
                        barData.add(new GraphData(value, labels[i]));
                }

                return ResponseEntity.ok(barData);
        }

        @GetMapping("/transactions/stats/year")
        public ResponseEntity<StatsData> getYearStats() {
                String user = getCurrentUser();
                LocalDate today = LocalDate.now();
                int currentYear = today.getYear();

                List<Transaction> expenses = transactionRepository.findByUserId(user).stream()
                                .filter(t -> t.transactionType() == TransactionType.Expense)
                                .collect(Collectors.toList());

                // --- Graph Data (filtered to current year) ---
                Map<Month, BigDecimal> monthlySums = expenses.stream()
                                .filter(t -> t.transaction_date().getYear() == currentYear)
                                .collect(Collectors.groupingBy(
                                                t -> t.transaction_date().getMonth(),
                                                Collectors.reducing(BigDecimal.ZERO, Transaction::amount,
                                                                BigDecimal::add)));

                String[] labels = { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov",
                                "Dec" };
                List<GraphData> barData = new ArrayList<>();

                for (int i = 0; i < 12; i++) {
                        Month month = Month.of(i + 1);
                        BigDecimal value = monthlySums.getOrDefault(month, BigDecimal.ZERO);

                        barData.add(month.equals(today.getMonth())
                                        ? new GraphData(value, labels[i], "#ffff")
                                        : new GraphData(value, labels[i]));
                }

                // --- Stats ---
                BigDecimal total = monthlySums.values().stream()
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Top spending month (from current year only)
                Map.Entry<Month, BigDecimal> topMonth = monthlySums.entrySet().stream()
                                .max(Map.Entry.comparingByValue())
                                .orElse(null);

                String topSpending = topMonth != null
                                ? topMonth.getKey().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                                : "N/A";
                BigDecimal topSpendingAmount = topMonth != null ? topMonth.getValue() : BigDecimal.ZERO;

                // Change since last year (%)
                BigDecimal lastYearTotal = expenses.stream()
                                .filter(t -> t.transaction_date().getYear() == currentYear - 1)
                                .map(Transaction::amount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                Integer changeSinceLast = lastYearTotal.compareTo(BigDecimal.ZERO) == 0 ? 0
                                : total.subtract(lastYearTotal)
                                                .divide(lastYearTotal, 2, RoundingMode.HALF_UP)
                                                .multiply(BigDecimal.valueOf(100))
                                                .max(BigDecimal.valueOf(-999)) // floor
                                                .min(BigDecimal.valueOf(999)) // cap
                                                .intValue();

                return ResponseEntity
                                .ok(new StatsData(total, changeSinceLast, topSpending, topSpendingAmount, barData));
        }

}
