package org.example.expenseapi.service;

import org.example.expenseapi.model.ExpenseCategory;

import java.util.List;
import java.util.Optional;

public interface ExpenseCategoryService {
    ExpenseCategory createExpenseCategory(ExpenseCategory category);
    ExpenseCategory updateExpenseCategory(Long id, ExpenseCategory category);
    void deleteExpenseCategory(Long id);
    Optional<ExpenseCategory> findById(Long id);
    List<ExpenseCategory> findAll();
    Optional<ExpenseCategory> findByName(String name);
}

