package org.example.expenseapi.controller;

import org.example.expenseapi.dto.ExpenseUpdateRequest;
import org.example.expenseapi.model.Expense;
import org.example.expenseapi.model.ExpenseStatus;
import org.example.expenseapi.service.ExpenseBudgetService;
import org.example.expenseapi.service.ExpenseCategoryService;
import org.example.expenseapi.service.ExpenseService;
import org.example.expenseapi.service.ExpenseStatusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class ExpenseControllerTest {

    private ExpenseService expenseService;
    private ExpenseCategoryService categoryService;
    private ExpenseStatusService statusService;
    private ExpenseBudgetService budgetService;
    private ExpenseController controller;

    @BeforeEach
    void setUp() {
        expenseService = Mockito.mock(ExpenseService.class);
        categoryService = Mockito.mock(ExpenseCategoryService.class);
        statusService = Mockito.mock(ExpenseStatusService.class);
        budgetService = Mockito.mock(ExpenseBudgetService.class);

        controller = new ExpenseController(expenseService, categoryService, statusService, budgetService);
    }

    @Test
    void update_whenCategoryIdProvidedButNotFound_returnsBadRequest() {
        // given
        Long expenseId = 1L;
        ExpenseUpdateRequest req = new ExpenseUpdateRequest();
        req.setExpenseCategoryId(99L); // non-existing
        req.setExpenseDate(LocalDate.now());
        req.setDesignation("test");
        req.setAmount(BigDecimal.valueOf(10.00));

        Mockito.when(expenseService.findById(expenseId)).thenReturn(Optional.of(new Expense()));
        Mockito.when(categoryService.findById(99L)).thenReturn(Optional.empty());

        // when
        ResponseEntity<?> resp = controller.update(expenseId, req);

        // then
        assertEquals(400, resp.getStatusCodeValue());
        assertNotNull(resp.getBody());
        String body = resp.getBody().toString();
        assertTrue(body.contains("Category not found"));
    }

    @Test
    void update_whenStatusIdProvidedButNotFound_returnsBadRequest() {
        // given
        Long expenseId = 2L;
        ExpenseUpdateRequest req = new ExpenseUpdateRequest();
        req.setExpenseStatusId(77L); // non-existing
        req.setExpenseDate(LocalDate.now());
        req.setDesignation("test2");
        req.setAmount(BigDecimal.valueOf(20.00));

        Mockito.when(expenseService.findById(expenseId)).thenReturn(Optional.of(new Expense()));
        Mockito.when(statusService.findById(77L)).thenReturn(Optional.empty());

        // when
        ResponseEntity<?> resp = controller.update(expenseId, req);

        // then
        assertEquals(400, resp.getStatusCodeValue());
        assertNotNull(resp.getBody());
        String body = resp.getBody().toString();
        assertTrue(body.contains("Status not found"));
    }

    @Test
    void update_whenBothIdsProvidedAndExist_callsServiceUpdateAndReturnsOk() {
        // given
        Long expenseId = 3L;
        ExpenseUpdateRequest req = new ExpenseUpdateRequest();
        req.setExpenseCategoryId(10L);
        req.setExpenseStatusId(20L);
        req.setExpenseDate(LocalDate.now());
        req.setDesignation("ok");
        req.setAmount(BigDecimal.valueOf(30.00));

        Expense existing = new Expense();
        existing.setId(expenseId);

        Mockito.when(expenseService.findById(expenseId)).thenReturn(Optional.of(existing));
        org.example.expenseapi.model.ExpenseCategory cat = new org.example.expenseapi.model.ExpenseCategory();
        cat.setId(10L);
        Mockito.when(categoryService.findById(10L)).thenReturn(Optional.of(cat));
        ExpenseStatus st = new ExpenseStatus(); st.setId(20L);
        Mockito.when(statusService.findById(20L)).thenReturn(Optional.of(st));

        Expense saved = new Expense(); saved.setId(expenseId);
        Mockito.when(expenseService.updateExpense(ArgumentMatchers.eq(expenseId), ArgumentMatchers.any(Expense.class))).thenReturn(saved);

        // when
        ResponseEntity<?> resp = controller.update(expenseId, req);

        // then
        assertEquals(200, resp.getStatusCodeValue());
        assertNotNull(resp.getBody());
    }
}

