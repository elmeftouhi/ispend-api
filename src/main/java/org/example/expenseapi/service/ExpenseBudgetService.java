package org.example.expenseapi.service;

import org.example.expenseapi.dto.BudgetStatus;
import org.example.expenseapi.dto.ExpenseCategoryBudgetDto;
import org.example.expenseapi.model.ExpenseCategoryBudget;
import org.example.expenseapi.repository.ExpenseCategoryBudgetRepository;
import org.example.expenseapi.repository.ExpenseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@Service
public class ExpenseBudgetService {

    private static final Logger log = LoggerFactory.getLogger(ExpenseBudgetService.class);

    private final ExpenseCategoryBudgetRepository budgetRepo;
    private final ExpenseRepository expenseRepo;

    public ExpenseBudgetService(ExpenseCategoryBudgetRepository budgetRepo, ExpenseRepository expenseRepo) {
        this.budgetRepo = budgetRepo;
        this.expenseRepo = expenseRepo;
    }

    @Transactional
    public ExpenseCategoryBudget setBudget(Long categoryId, int year, int month, BigDecimal amount) {
        Optional<ExpenseCategoryBudget> opt = budgetRepo.findByCategoryIdAndYearAndMonth(categoryId, year, month);
        ExpenseCategoryBudget b = opt.orElseGet(ExpenseCategoryBudget::new);
        b.setYear(year);
        b.setMonth(month);
        b.setBudget(amount);
        // set category by id (load a stub)
        org.example.expenseapi.model.ExpenseCategory c = new org.example.expenseapi.model.ExpenseCategory();
        c.setId(categoryId);
        b.setCategory(c);
        return budgetRepo.save(b);
    }

    @Transactional
    public ExpenseCategoryBudget setAllowOverspendForBudget(Long budgetId, Boolean allow) {
        var opt = budgetRepo.findById(budgetId);
        if (opt.isEmpty()) return null;
        var b = opt.get();
        b.setAllowOverspend(allow == null ? Boolean.FALSE : allow);
        return budgetRepo.save(b);
    }

    public BudgetStatus getBudgetStatus(Long categoryId, int year, int month) {
        BigDecimal spent = sumSpentForMonth(categoryId, year, month);
        BudgetStatus s = new BudgetStatus();
        Optional<ExpenseCategoryBudget> opt = budgetRepo.findByCategoryIdAndYearAndMonth(categoryId, year, month);
        // If there is no budget entry for this category/month, do NOT compare expenses to a budget.
        if (opt.isEmpty()) {
            s.setBudget(null);
            s.setSpent(spent);
            s.setRemaining(null);
            s.setAllowOverspend(null);
            s.setOverBudget(false); // no budget => cannot be over budget
            log.debug("getBudgetStatus: categoryId={} year={} month={} -> NO BUDGET (spent={}), overBudget=false", categoryId, year, month, spent);
            return s;
        }

        // Budget exists: compute normally
        BigDecimal budget = opt.map(ExpenseCategoryBudget::getBudget).orElse(BigDecimal.ZERO);
        s.setBudget(budget);
        s.setSpent(spent);
        s.setRemaining(budget.subtract(spent));
        boolean allow = opt.map(ExpenseCategoryBudget::getAllowOverspend).orElse(Boolean.TRUE);
        boolean over = false;
        if (!allow) {
            // only compare if overspend is NOT allowed
            over = spent.compareTo(budget) > 0;
        }
        s.setOverBudget(over);
        s.setAllowOverspend(allow);
        log.debug("getBudgetStatus: categoryId={}, year={}, month={}, budget={}, spent={}, allowOverspend={}, overBudget={}", categoryId, year, month, budget, spent, allow, over);
        return s;
    }

    // Expose optional budget entity for a specific category/year/month so callers can check allowOverspend and other details
    public Optional<ExpenseCategoryBudget> findBudgetForCategoryMonth(Long categoryId, int year, int month) {
        return budgetRepo.findByCategoryIdAndYearAndMonth(categoryId, year, month);
    }

    public boolean isWithinBudgetForNewExpense(Long categoryId, LocalDate expenseDate, BigDecimal amount) {
        YearMonth ym = YearMonth.from(expenseDate);
        Optional<ExpenseCategoryBudget> budgetOpt = budgetRepo.findByCategoryIdAndYearAndMonth(categoryId, ym.getYear(), ym.getMonthValue());
        if (budgetOpt.isEmpty()) {
            return true; // no budget set => allow
        }
        ExpenseCategoryBudget b = budgetOpt.get();
        BigDecimal budget = b.getBudget();
        // if allowOverspend is true for this budget, allow creating expenses beyond the budget
        if (b.getAllowOverspend() != null && b.getAllowOverspend()) {
            return true;
        }
        BigDecimal spent = sumSpentForMonth(categoryId, ym.getYear(), ym.getMonthValue());
        return spent.add(amount).compareTo(budget) <= 0;
    }

    public List<ExpenseCategoryBudget> findBudgetsForCategory(Long categoryId) {
        return budgetRepo.findByCategoryIdOrderByYearAscMonthAsc(categoryId);
    }

    @Transactional
    public void deleteBudget(Long categoryId, int year, int month) {
        var opt = budgetRepo.findByCategoryIdAndYearAndMonth(categoryId, year, month);
        opt.ifPresent(budgetRepo::delete);
    }

    public List<ExpenseCategoryBudgetDto> findBudgetsForCategoryDto(Long categoryId) {
        var entities = findBudgetsForCategory(categoryId);
        List<ExpenseCategoryBudgetDto> res = new ArrayList<>();
        for (var b : entities) {
            var dto = new ExpenseCategoryBudgetDto();
            dto.setYear(b.getYear()); dto.setMonth(b.getMonth()); dto.setBudget(b.getBudget());
            dto.setAllowOverspend(b.getAllowOverspend());
            res.add(dto);
        }
        return res;
    }

    /**
     * Batch compute BudgetStatus for a list of category ids for a specific year and month.
     * Returns a map categoryId -> BudgetStatus. Categories with no budget entry will still have a BudgetStatus with budget=0 and spent=0.
     */
    public Map<Long, BudgetStatus> getBudgetStatusForCategories(java.util.List<Long> categoryIds, int year, int month) {
        Map<Long, BudgetStatus> result = new HashMap<>();
        if (categoryIds == null) return result;

        // sanitize category ids: remove nulls and duplicates
        java.util.List<Long> ids = new java.util.ArrayList<>(categoryIds);
        ids.removeIf(java.util.Objects::isNull);
        if (ids.isEmpty()) return result;
        ids = new java.util.ArrayList<>(new java.util.LinkedHashSet<>(ids));

        // Build the date range for the month
        java.time.LocalDate start = java.time.LocalDate.of(year, month, 1);
        java.time.LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        // 1) Fetch sums per category in the date range
        var sums = expenseRepo.sumAmountGroupedByCategoryBetween(start, end, ids);
        Map<Long, java.math.BigDecimal> spentMap = new HashMap<>();
        if (sums != null) {
            for (Object[] row : sums) {
                Long catId = row[0] == null ? null : ((Number) row[0]).longValue();
                java.math.BigDecimal sum = row[1] == null ? java.math.BigDecimal.ZERO : (java.math.BigDecimal) row[1];
                if (catId != null) spentMap.put(catId, sum);
            }
        }

        // 2) Fetch budgets for the categories for the same period
        var budgets = budgetRepo.findByCategoryIdInAndYearAndMonth(ids, year, month);
        Map<Long, org.example.expenseapi.model.ExpenseCategoryBudget> budgetMap = new HashMap<>();
        if (budgets != null) {
            for (var b : budgets) {
                if (b.getCategory() != null && b.getCategory().getId() != null) {
                    budgetMap.put(b.getCategory().getId(), b);
                }
            }
        }

        // Debug logs to help trace mismatched values
        log.debug("getBudgetStatusForCategories: year={}, month={}, requestedIds={}, spentMap={}, budgetMapKeys={}", year, month, ids, spentMap, budgetMap.keySet());

        // 3) Compose BudgetStatus for each requested category
        for (Long cid : ids) {
            BudgetStatus s = new BudgetStatus();
            java.math.BigDecimal spent = spentMap.getOrDefault(cid, java.math.BigDecimal.ZERO);
            var optB = java.util.Optional.ofNullable(budgetMap.get(cid));
            // If no budget entity exists for this category/month, do not compare expenses
            if (optB.isEmpty()) {
                s.setBudget(null);
                s.setSpent(spent);
                s.setRemaining(null);
                s.setAllowOverspend(null);
                s.setOverBudget(false);
                log.debug("getBudgetStatusForCategories: categoryId={} -> NO BUDGET (spent={}), overBudget=false", cid, spent);
                result.put(cid, s);
                continue;
            }

            java.math.BigDecimal budget = optB.map(org.example.expenseapi.model.ExpenseCategoryBudget::getBudget).orElse(java.math.BigDecimal.ZERO);
            s.setBudget(budget);
            s.setSpent(spent);
            s.setRemaining(budget.subtract(spent));
            boolean allowLocal = optB.map(org.example.expenseapi.model.ExpenseCategoryBudget::getAllowOverspend).orElse(Boolean.TRUE);
            boolean overLocal = false;
            if (!allowLocal) {
                overLocal = spent.compareTo(budget) > 0;
            }
            s.setOverBudget(overLocal);
            s.setAllowOverspend(allowLocal);
            log.debug("getBudgetStatusForCategories: categoryId={}, budget={}, spent={}, allowOverspend={}, overBudget={}", cid, budget, spent, allowLocal, overLocal);
             result.put(cid, s);
         }

         return result;
     }

    private BigDecimal sumSpentForMonth(Long categoryId, int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        BigDecimal sum = expenseRepo.sumAmountByCategoryAndDateBetween(categoryId, start, end);
        return sum == null ? BigDecimal.ZERO : sum;
    }
}
