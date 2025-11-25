package org.example.expenseapi.config;

import org.example.expenseapi.model.Expense;
import org.example.expenseapi.model.ExpenseCategory;
import org.example.expenseapi.model.ExpenseStatus;
import org.example.expenseapi.repository.ExpenseCategoryRepository;
import org.example.expenseapi.repository.ExpenseRepository;
import org.example.expenseapi.repository.ExpenseStatusRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Value("${app.seed-data.enabled:false}")
    private boolean seedEnabled;

    private final ExpenseStatusRepository statusRepository;
    private final ExpenseCategoryRepository categoryRepository;
    private final ExpenseRepository expenseRepository;

    public DataSeeder(ExpenseStatusRepository statusRepository,
                      ExpenseCategoryRepository categoryRepository,
                      ExpenseRepository expenseRepository) {
        this.statusRepository = statusRepository;
        this.categoryRepository = categoryRepository;
        this.expenseRepository = expenseRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!seedEnabled) {
            log.info("Data seeding disabled (app.seed-data.enabled is false)");
            return;
        }

        log.info("Data seeding enabled - checking existing data...");

        // Seed statuses if missing
        seedStatuses();

        // Seed categories if missing
        seedCategories();

        // Seed expenses if none exist
        if (expenseRepository.count() == 0) {
            seedExpenses();
        } else {
            log.info("Expenses already present (count={}) - skipping expense seeding", expenseRepository.count());
        }
    }

    private void seedStatuses() {
        List<String> defaultStatuses = List.of("Pending", "Approved", "Rejected");
        for (String name : defaultStatuses) {
            Optional<ExpenseStatus> found = statusRepository.findByName(name);
            if (found.isEmpty()) {
                ExpenseStatus s = new ExpenseStatus();
                s.setName(name);
                s.setIsDefault("Pending".equals(name)); // make Pending default
                statusRepository.save(s);
                log.info("Created ExpenseStatus: {}", name);
            } else {
                log.info("ExpenseStatus '{}' already exists", name);
            }
        }
    }

    private void seedCategories() {
        List<String> categories = List.of("Food", "Transport", "Utilities", "Office", "Entertainment");
        for (String name : categories) {
            Optional<ExpenseCategory> found = categoryRepository.findByName(name);
            if (found.isEmpty()) {
                ExpenseCategory c = new ExpenseCategory();
                c.setName(name);
                c.setStatus(org.example.expenseapi.model.ExpenseCategoryStatus.ACTIVE);
                c.setLevel(0);
                categoryRepository.save(c);
                log.info("Created ExpenseCategory: {}", name);
            } else {
                log.info("ExpenseCategory '{}' already exists", name);
            }
        }
    }

    private void seedExpenses() {
        log.info("Seeding expenses for current and previous year...");

        var statuses = statusRepository.findAll();
        var categories = categoryRepository.findAll();
        if (statuses.isEmpty() || categories.isEmpty()) {
            log.warn("Cannot seed expenses - statuses or categories are missing");
            return;
        }

        ExpenseStatus defaultStatus = statuses.get(0);
        ExpenseCategory defaultCategory = categories.get(0);

        List<Expense> toSave = new ArrayList<>();

        LocalDate today = LocalDate.now();
        int currentYear = today.getYear();

        // create expenses from Jan 1 currentYear to today: one per week
        LocalDate d = LocalDate.of(currentYear, Month.JANUARY, 1);
        while (!d.isAfter(today)) {
            Expense ex = new Expense();
            ex.setExpenseDate(d);
            ex.setDesignation("Seed expense " + d);
            ex.setExpenseCategory(defaultCategory);
            ex.setExpenseStatus(defaultStatus);
            ex.setAmount(BigDecimal.valueOf(10 + (d.getMonthValue() % 10)));
            toSave.add(ex);
            d = d.plusWeeks(1);
        }

        // create some expenses for last year (one per month)
        LocalDate jan1LastYear = LocalDate.of(currentYear - 1, Month.JANUARY, 1);
        for (int m = 0; m < 12; m++) {
            LocalDate date = jan1LastYear.plusMonths(m).withDayOfMonth(15);
            Expense ex = new Expense();
            ex.setExpenseDate(date);
            ex.setDesignation("Seed last year " + date);
            ex.setExpenseCategory(categories.get((m % categories.size())));
            ex.setExpenseStatus(statuses.get((m % statuses.size())));
            ex.setAmount(BigDecimal.valueOf(20 + m));
            toSave.add(ex);
        }

        expenseRepository.saveAll(toSave);
        log.info("Saved {} seed expenses", toSave.size());
    }
}
