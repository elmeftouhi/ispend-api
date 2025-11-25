package org.example.expenseapi.service;

import org.example.expenseapi.model.Expense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ExpenseService {
    Expense createExpense(Expense expense);
    Expense updateExpense(Long id, Expense expense);
    void deleteExpense(Long id);
    Optional<Expense> findById(Long id);
    List<Expense> findAll();

    // paged retrieval
    Page<Expense> findAll(Pageable pageable);

    // New: search with optional keyword, category ids (nullable list), and date range
    Page<Expense> search(String keyword, List<Long> categoryIds, java.time.LocalDate start, java.time.LocalDate end, Pageable pageable);

    // New: reports grouped by year -> month; accepts optional single-category filter via categoryIds
    java.util.List<org.example.expenseapi.dto.YearlyExpenseDto> getExpensesReportByCategory(java.time.LocalDate start, java.time.LocalDate end, java.util.List<Long> categoryIds);
}
