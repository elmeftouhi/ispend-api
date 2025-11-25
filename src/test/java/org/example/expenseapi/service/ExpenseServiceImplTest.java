package org.example.expenseapi.service;

import org.example.expenseapi.model.Expense;
import org.example.expenseapi.model.ExpenseCategory;
import org.example.expenseapi.model.ExpenseStatus;
import org.example.expenseapi.repository.ExpenseCategoryRepository;
import org.example.expenseapi.repository.ExpenseRepository;
import org.example.expenseapi.repository.ExpenseStatusRepository;
import org.example.expenseapi.service.impl.ExpenseServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ExpenseServiceImplTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private ExpenseCategoryRepository categoryRepository;

    @Mock
    private ExpenseStatusRepository statusRepository;

    @InjectMocks
    private ExpenseServiceImpl service;

    @BeforeEach
    void setUp() {
        // no-op: MockitoExtension handles mock initialization
    }

    @Test
    void createExpense_success() {
        ExpenseCategory cat = new ExpenseCategory();
        cat.setId(1L);
        ExpenseStatus st = new ExpenseStatus();
        st.setId(2L);

        Expense toCreate = new Expense();
        toCreate.setDesignation("Lunch");
        toCreate.setAmount(new BigDecimal("12.50"));
        toCreate.setExpenseDate(LocalDate.of(2025, 10, 1));
        toCreate.setExpenseCategory(cat);
        toCreate.setExpenseStatus(st);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(cat));
        when(statusRepository.findById(2L)).thenReturn(Optional.of(st));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(i -> i.getArgument(0));

        Expense created = service.createExpense(toCreate);

        assertNotNull(created);
        verify(categoryRepository).findById(1L);
        verify(statusRepository).findById(2L);
        verify(expenseRepository).save(any(Expense.class));
        assertEquals("Lunch", created.getDesignation());
    }

    @Test
    void createExpense_missingCategory_throws() {
        ExpenseStatus st = new ExpenseStatus();
        st.setId(2L);

        Expense toCreate = new Expense();
        toCreate.setDesignation("Dinner");
        toCreate.setAmount(new BigDecimal("20.00"));
        toCreate.setExpenseDate(LocalDate.of(2025, 10, 2));
        toCreate.setExpenseStatus(st);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.createExpense(toCreate));
        assertThat(ex.getMessage()).contains("ExpenseCategory is required");
    }

    @Test
    void updateExpense_success() {
        Expense existing = new Expense();
        when(expenseRepository.findById(100L)).thenReturn(Optional.of(existing));

        ExpenseCategory cat = new ExpenseCategory();
        cat.setId(5L);
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(cat));

        ExpenseStatus st = new ExpenseStatus();
        st.setId(7L);
        when(statusRepository.findById(7L)).thenReturn(Optional.of(st));

        Expense update = new Expense();
        update.setDesignation("Taxi");
        update.setAmount(new BigDecimal("15.00"));
        update.setExpenseCategory(cat);
        update.setExpenseStatus(st);

        when(expenseRepository.save(any(Expense.class))).thenAnswer(i -> i.getArgument(0));

        Expense result = service.updateExpense(100L, update);

        assertNotNull(result);
        assertEquals("Taxi", result.getDesignation());
        assertEquals(new BigDecimal("15.00"), result.getAmount());
        verify(categoryRepository).findById(5L);
        verify(statusRepository).findById(7L);
        verify(expenseRepository).save(existing);
    }

    @Test
    void updateExpense_notFound_throws() {
        when(expenseRepository.findById(200L)).thenReturn(Optional.empty());
        Expense update = new Expense();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.updateExpense(200L, update));
        assertThat(ex.getMessage()).contains("Expense not found");
    }

    @Test
    void findById_delegates() {
        Expense e = new Expense();
        when(expenseRepository.findById(10L)).thenReturn(Optional.of(e));
        Optional<Expense> res = service.findById(10L);
        assertTrue(res.isPresent());
        assertEquals(e, res.get());
        verify(expenseRepository).findById(10L);
    }

    @Test
    void deleteExpense_delegates() {
        doNothing().when(expenseRepository).deleteById(50L);
        service.deleteExpense(50L);
        verify(expenseRepository).deleteById(50L);
    }

    @Test
    void findAll_returnsSortedList() {
        Expense e1 = new Expense();
        Expense e2 = new Expense();
        when(expenseRepository.findAll(any(org.springframework.data.domain.Sort.class))).thenReturn(List.of(e1, e2));
        List<Expense> all = service.findAll();
        assertThat(all).hasSize(2);
        verify(expenseRepository).findAll(any(org.springframework.data.domain.Sort.class));
    }

    @Test
    void findAll_paged_delegates() {
        Expense e = new Expense();
        Page<Expense> page = new PageImpl<>(List.of(e));
        Pageable pageable = PageRequest.of(0, 10);
        when(expenseRepository.findAll(pageable)).thenReturn(page);
        Page<Expense> res = service.findAll(pageable);
        assertThat(res.getContent()).contains(e);
        verify(expenseRepository).findAll(pageable);
    }

    @Test
    void search_delegatesWithNulls() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Expense> page = new PageImpl<>(List.of());
        when(expenseRepository.search(null, null, null, null, pageable)).thenReturn(page);
        Page<Expense> res = service.search(null, null, null, null, pageable);
        assertThat(res.getTotalElements()).isEqualTo(0);
        verify(expenseRepository).search(null, null, null, null, pageable);
    }

    @Test
    void search_emptyCategoryIds_treatedAsNull() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Expense> page = new PageImpl<>(List.of());
        when(expenseRepository.search("x", null, null, null, pageable)).thenReturn(page);
        Page<Expense> res = service.search("x", Collections.emptyList(), null, null, pageable);
        assertThat(res.getTotalElements()).isEqualTo(0);
        verify(expenseRepository).search("x", null, null, null, pageable);
    }
}
