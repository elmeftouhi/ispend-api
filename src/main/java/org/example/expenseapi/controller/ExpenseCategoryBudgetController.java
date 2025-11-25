package org.example.expenseapi.controller;

import jakarta.validation.Valid;
import org.example.expenseapi.application.service.ExpenseCategoryApplicationService;
import org.example.expenseapi.dto.ExpenseCategoryBudgetCreateRequest;
import org.example.expenseapi.dto.ExpenseCategoryBudgetDto;
import org.example.expenseapi.dto.BudgetStatus;
import org.example.expenseapi.model.ExpenseCategoryBudget;
import org.example.expenseapi.service.ExpenseBudgetService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/expense-categories/{categoryId}/budgets")
public class ExpenseCategoryBudgetController {

    private final ExpenseBudgetService budgetService;
    private final ExpenseCategoryApplicationService categoryAppService;

    public ExpenseCategoryBudgetController(ExpenseBudgetService budgetService, ExpenseCategoryApplicationService categoryAppService) {
        this.budgetService = budgetService;
        this.categoryAppService = categoryAppService;
    }

    @GetMapping
    public ResponseEntity<?> list(@PathVariable Long categoryId) {
        // ensure category exists
        if (categoryId == null || categoryAppService.findById(categoryId).isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Category not found"));
        }
        var dtos = budgetService.findBudgetsForCategoryDto(categoryId);
        return ResponseEntity.ok(dtos);
    }

    @PostMapping
    public ResponseEntity<?> createOrUpdate(@PathVariable Long categoryId, @Valid @RequestBody ExpenseCategoryBudgetCreateRequest req) {
        // validate category
        if (categoryId == null || categoryAppService.findById(categoryId).isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Category not found"));
        }
        // basic validation
        if (req == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Request body is required"));
        }
        int year = req.getYear();
        int month = req.getMonth();
        BigDecimal budget = req.getBudget();
        if (year < 1970 || year > 9999) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Year is invalid"));
        }
        if (month < 1 || month > 12) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Month must be between 1 and 12"));
        }
        if (budget == null || budget.compareTo(BigDecimal.ZERO) < 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Budget must be non-null and non-negative"));
        }

        ExpenseCategoryBudget saved = budgetService.setBudget(categoryId, year, month, budget);
        // set allowOverspend if provided in the request
        if (req.getAllowOverspend() != null) {
            saved.setAllowOverspend(req.getAllowOverspend());
            saved = budgetService.setAllowOverspendForBudget(saved.getId(), req.getAllowOverspend());
        }
        ExpenseCategoryBudgetDto dto = new ExpenseCategoryBudgetDto();
        dto.setYear(saved.getYear()); dto.setMonth(saved.getMonth()); dto.setBudget(saved.getBudget());
        dto.setAllowOverspend(saved.getAllowOverspend());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/{year}/{month}/status")
    public ResponseEntity<?> status(@PathVariable Long categoryId, @PathVariable int year, @PathVariable int month) {
        if (categoryId == null || categoryAppService.findById(categoryId).isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Category not found"));
        }
        if (year < 1970 || year > 9999) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Year is invalid"));
        }
        if (month < 1 || month > 12) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Month must be between 1 and 12"));
        }
        BudgetStatus s = budgetService.getBudgetStatus(categoryId, year, month);
        return ResponseEntity.ok(s);
    }

    @DeleteMapping("/{year}/{month}")
    public ResponseEntity<?> delete(@PathVariable Long categoryId, @PathVariable int year, @PathVariable int month) {
        if (categoryId == null || categoryAppService.findById(categoryId).isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Category not found"));
        }
        if (year < 1970 || year > 9999) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Year is invalid"));
        }
        if (month < 1 || month > 12) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Month must be between 1 and 12"));
        }
        budgetService.deleteBudget(categoryId, year, month);
        return ResponseEntity.noContent().build();
    }
}
