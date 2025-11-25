package org.example.expenseapi.repository;

import org.example.expenseapi.model.ExpenseCategoryBudget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExpenseCategoryBudgetRepository extends JpaRepository<ExpenseCategoryBudget, Long> {
    Optional<ExpenseCategoryBudget> findByCategoryIdAndYearAndMonth(Long categoryId, int year, int month);
    List<ExpenseCategoryBudget> findByCategoryIdOrderByYearAscMonthAsc(Long categoryId);
    // Batch lookup for budgets for many categories for a specific year and month
    List<ExpenseCategoryBudget> findByCategoryIdInAndYearAndMonth(List<Long> categoryIds, int year, int month);
}
