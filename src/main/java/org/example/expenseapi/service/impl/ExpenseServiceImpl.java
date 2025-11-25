package org.example.expenseapi.service.impl;

import org.example.expenseapi.model.Expense;
import org.example.expenseapi.model.ExpenseCategory;
import org.example.expenseapi.model.ExpenseStatus;
import org.example.expenseapi.repository.ExpenseRepository;
import org.example.expenseapi.repository.ExpenseCategoryRepository;
import org.example.expenseapi.repository.ExpenseStatusRepository;
import org.example.expenseapi.service.ExpenseService;
import org.example.expenseapi.service.UserService;
import org.example.expenseapi.application.service.UserSettingsApplicationService;
import org.example.expenseapi.model.UserSettings;
import org.example.expenseapi.util.CurrencyFormatter;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Locale;

@Service
@Transactional
public class ExpenseServiceImpl implements ExpenseService {

    private final ExpenseRepository repository;
    private final ExpenseCategoryRepository categoryRepository;
    private final ExpenseStatusRepository statusRepository;
    private final UserService userService;
    private final UserSettingsApplicationService userSettingsService;

    public ExpenseServiceImpl(ExpenseRepository repository,
                              ExpenseCategoryRepository categoryRepository,
                              ExpenseStatusRepository statusRepository,
                              UserService userService,
                              UserSettingsApplicationService userSettingsService) {
        this.repository = repository;
        this.categoryRepository = categoryRepository;
        this.statusRepository = statusRepository;
        this.userService = userService;
        this.userSettingsService = userSettingsService;
    }

    @Override
    public Expense createExpense(Expense expense) {
        // ensure referenced category and status exist
        ExpenseCategory cat = null;
        ExpenseStatus st = null;
        if (expense.getExpenseCategory() != null && expense.getExpenseCategory().getId() != null) {
            cat = categoryRepository.findById(expense.getExpenseCategory().getId())
                    .orElseThrow(() -> new RuntimeException("ExpenseCategory not found: " + expense.getExpenseCategory().getId()));
            expense.setExpenseCategory(cat);
        } else {
            throw new RuntimeException("ExpenseCategory is required");
        }

        if (expense.getExpenseStatus() != null && expense.getExpenseStatus().getId() != null) {
            st = statusRepository.findById(expense.getExpenseStatus().getId())
                    .orElseThrow(() -> new RuntimeException("ExpenseStatus not found: " + expense.getExpenseStatus().getId()));
            expense.setExpenseStatus(st);
        } else {
            throw new RuntimeException("ExpenseStatus is required");
        }

        return repository.save(expense);
    }

    @Override
    public Expense updateExpense(Long id, Expense expense) {
        Expense existing = repository.findById(id).orElseThrow(() -> new RuntimeException("Expense not found: " + id));

        if (expense.getExpenseDate() != null) existing.setExpenseDate(expense.getExpenseDate());
        if (expense.getDesignation() != null) existing.setDesignation(expense.getDesignation());
        if (expense.getAmount() != null) existing.setAmount(expense.getAmount());

        if (expense.getExpenseCategory() != null && expense.getExpenseCategory().getId() != null) {
            ExpenseCategory cat = categoryRepository.findById(expense.getExpenseCategory().getId())
                    .orElseThrow(() -> new RuntimeException("ExpenseCategory not found: " + expense.getExpenseCategory().getId()));
            existing.setExpenseCategory(cat);
        }

        if (expense.getExpenseStatus() != null && expense.getExpenseStatus().getId() != null) {
            ExpenseStatus st = statusRepository.findById(expense.getExpenseStatus().getId())
                    .orElseThrow(() -> new RuntimeException("ExpenseStatus not found: " + expense.getExpenseStatus().getId()));
            existing.setExpenseStatus(st);
        }

        return repository.save(existing);
    }

    @Override
    public void deleteExpense(Long id) {
        repository.deleteById(id);
    }

    @Override
    public Optional<Expense> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public List<Expense> findAll() {
        // return all results ordered by expenseDate descending by default
        return repository.findAll(Sort.by(Sort.Direction.DESC, "expenseDate"));
    }

    @Override
    public org.springframework.data.domain.Page<Expense> findAll(org.springframework.data.domain.Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Override
    public org.springframework.data.domain.Page<Expense> search(String keyword, List<Long> categoryIds, java.time.LocalDate start, java.time.LocalDate end, org.springframework.data.domain.Pageable pageable) {
        // normalize keyword: treat blank as null
        String k = (keyword == null || keyword.isBlank()) ? null : keyword.trim();

        // sanitize category ids: remove nulls/duplicates and treat empty as null
        List<Long> cats = (categoryIds == null || categoryIds.isEmpty()) ? null : new java.util.ArrayList<>(categoryIds);
        if (cats != null) {
            cats.removeIf(java.util.Objects::isNull);
            if (cats.isEmpty()) cats = null;
            else cats = new java.util.ArrayList<>(new java.util.LinkedHashSet<>(cats));
        }

        // Delegate to repository search (tests expect this behavior)
        return repository.search(k, cats, start, end, pageable);
    }

    private List<Expense> findAllByExpenseDateBetweenOrderByExpenseDateDesc(java.time.LocalDate start, java.time.LocalDate end) {
        if (start == null) {
            start = LocalDate.of(1970,1,1);
        }
        if (end == null) {
            end = LocalDate.now();
        }
        return repository.findAllByExpenseDateBetweenOrderByExpenseDateDesc(start, end);
    }

    // Helper to get current authenticated user's email (username)
    private String getCurrentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        Object principal = auth.getPrincipal();
        if (principal instanceof UserDetails) return ((UserDetails) principal).getUsername();
        if (principal instanceof String) return (String) principal;
        return null;
    }

    // Helper to format amount based on current user's settings, fallback to default
    private String formatAmountForCurrentUser(BigDecimal amount) {
        try {
            String username = getCurrentUsername();
            Locale locale = Locale.getDefault();
            String currencyCode = null;
            Integer digits = null;
            String placement = null; // BEFORE or AFTER
            if (username != null) {
                var userOpt = userService.findByEmail(username);
                if (userOpt.isPresent()) {
                    var user = userOpt.get();
                    var settOpt = userSettingsService.findByUserId(user.getId());
                    if (settOpt.isPresent()) {
                        UserSettings s = settOpt.get();
                        currencyCode = s.getCurrency();
                        digits = s.getDecimalDigits();
                        placement = s.getCurrencySymbolPlacement();
                    }
                }
            }

            return CurrencyFormatter.format(amount, currencyCode, digits, placement, locale);
        } catch (Exception ex) {
            BigDecimal v = amount == null ? BigDecimal.ZERO : amount;
            return v.setScale(2, java.math.RoundingMode.HALF_UP).toString();
        }
    }

    @Override
    public java.util.List<org.example.expenseapi.dto.YearlyExpenseDto> getExpensesReportByCategory(java.time.LocalDate start, java.time.LocalDate end, java.util.List<Long> categoryIds) {
        // normalize dates
        if (start == null) start = LocalDate.of(1970,1,1);
        if (end == null) end = LocalDate.now();

        List<Expense> expenses = repository.findAllByExpenseDateBetweenOrderByExpenseDateDesc(start, end);

        // filter by category if provided (single category id expected)
        List<Expense> filtered = expenses.stream()
                .filter(e -> e.getExpenseCategory() != null && e.getExpenseCategory().getId() != null)
                .filter(e -> categoryIds == null || categoryIds.isEmpty() || categoryIds.contains(e.getExpenseCategory().getId()))
                .collect(Collectors.toList());

        // Now aggregate by year -> month across the filtered expenses (no further grouping by category)
        Map<Integer, Map<Integer, BigDecimal>> yearMonthSums = new HashMap<>();
        for (Expense ex : filtered) {
            if (ex.getExpenseDate() == null || ex.getAmount() == null) continue;
            int y = ex.getExpenseDate().getYear();
            int m = ex.getExpenseDate().getMonthValue();
            yearMonthSums.computeIfAbsent(y, yy -> new HashMap<>());
            Map<Integer, BigDecimal> months = yearMonthSums.get(y);
            months.put(m, months.getOrDefault(m, BigDecimal.ZERO).add(ex.getAmount()));
        }

        List<org.example.expenseapi.dto.YearlyExpenseDto> years = new ArrayList<>();
        for (Map.Entry<Integer, Map<Integer, BigDecimal>> ye : yearMonthSums.entrySet()) {
            Integer year = ye.getKey();
            Map<Integer, BigDecimal> months = ye.getValue();
            Map<Integer, BigDecimal> monthsWithZeros = new HashMap<>();
            for (int i = 1; i <= 12; i++) {
                monthsWithZeros.put(i, months.getOrDefault(i, BigDecimal.ZERO));
            }
            BigDecimal total = monthsWithZeros.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            org.example.expenseapi.dto.YearlyExpenseDto ydto = new org.example.expenseapi.dto.YearlyExpenseDto();
            ydto.setYear(year);
            ydto.setMonths(monthsWithZeros);
            ydto.setTotal(total);
            ydto.setTotalFormated(formatAmountForCurrentUser(total));
            years.add(ydto);
        }

        // If no data found, return a single year object with zeros for months and total.
        if (years.isEmpty()) {
            int defaultYear = end.getYear();
            Map<Integer, BigDecimal> monthsWithZeros = new HashMap<>();
            for (int i = 1; i <= 12; i++) monthsWithZeros.put(i, BigDecimal.ZERO);
            org.example.expenseapi.dto.YearlyExpenseDto ydto = new org.example.expenseapi.dto.YearlyExpenseDto();
            ydto.setYear(defaultYear);
            ydto.setMonths(monthsWithZeros);
            ydto.setTotal(BigDecimal.ZERO);
            ydto.setTotalFormated(formatAmountForCurrentUser(BigDecimal.ZERO));
            years.add(ydto);
        }

        // sort years descending
        years.sort((a,b) -> b.getYear().compareTo(a.getYear()));
        return years;
    }
}
