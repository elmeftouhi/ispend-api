package org.example.expenseapi.application.service;

import org.example.expenseapi.dto.ExpenseCategoryDto;
import org.example.expenseapi.model.ExpenseCategory;

import java.util.List;
import java.util.Optional;

public interface ExpenseCategoryApplicationService {
    ExpenseCategory create(ExpenseCategory category);
    ExpenseCategory update(Long id, ExpenseCategory category);
    void delete(Long id);
    Optional<ExpenseCategory> findById(Long id);
    List<ExpenseCategory> findAll();
    Optional<ExpenseCategory> findByName(String name);

    // If the provided id is a child, return its parent assembled with direct children (as DTO)
    Optional<ExpenseCategoryDto> parentWithChildrenIfChild(Long id);

    // If the provided id is a top-level parent, return its sibling top-level categories (as DTOs)
    List<ExpenseCategoryDto> siblingsForParent(Long id);
}
