package org.example.expenseapi.service;

import org.example.expenseapi.model.ExpenseStatus;

import java.util.List;
import java.util.Optional;

public interface ExpenseStatusService {
    ExpenseStatus createExpenseStatus(ExpenseStatus status);
    ExpenseStatus updateExpenseStatus(Long id, ExpenseStatus status);
    void deleteExpenseStatus(Long id);
    Optional<ExpenseStatus> findById(Long id);
    List<ExpenseStatus> findAll();
    Optional<ExpenseStatus> findByName(String name);
    Optional<ExpenseStatus> findDefaultStatus();
}
