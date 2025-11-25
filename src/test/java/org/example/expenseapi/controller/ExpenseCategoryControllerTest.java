package org.example.expenseapi.controller;

import org.example.expenseapi.application.service.ExpenseCategoryApplicationService;
import org.example.expenseapi.dto.ExpenseCategoryDto;
import org.example.expenseapi.model.ExpenseCategory;
import org.example.expenseapi.testutil.ExpenseCategoryBuilder;
import org.example.expenseapi.service.ExpenseBudgetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class ExpenseCategoryControllerTest {

    private MockMvc mockMvc;

    private final ExpenseCategoryApplicationService service = mock(ExpenseCategoryApplicationService.class);
    private final ExpenseBudgetService budgetService = mock(ExpenseBudgetService.class);

    @BeforeEach
    public void setup() {
        ExpenseCategoryController controller = new ExpenseCategoryController(service, budgetService);
        // stub budgets to return empty list for all categories used in tests
        when(budgetService.findBudgetsForCategory(anyLong())).thenReturn(List.of());
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new TestExceptionHandler())
                .build();
    }

    @Test
    public void getCategory_child_shouldReturnRequestedChildWithNoSubCategories() throws Exception {
        // parent and child models
        ExpenseCategory parent = ExpenseCategoryBuilder.anExpenseCategory().withId(10L).withName("Parent").build();
        ExpenseCategory child = ExpenseCategoryBuilder.anExpenseCategory().withId(5L).withName("Child").withParent(parent).build();

        // DTOs used only for expectations
        ExpenseCategoryDto childDto = new ExpenseCategoryDto();
        childDto.setId(5L);
        childDto.setName("Child");
        childDto.setParentId(10L);

        when(service.findById(5L)).thenReturn(Optional.of(child));
        when(service.findAll()).thenReturn(List.of(parent, child));

        mockMvc.perform(get("/v1/expense-categories/5").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.subCategories.length()").value(0));

        verify(service, times(1)).findById(5L);
        verify(service, times(1)).findAll();
        verify(service, never()).parentWithChildrenIfChild(anyLong());
    }

    @Test
    public void getCategory_parent_shouldReturnParentWithSubCategories() throws Exception {
        ExpenseCategory parent = ExpenseCategoryBuilder.anExpenseCategory().withId(10L).withName("Parent").build();
        ExpenseCategory child = ExpenseCategoryBuilder.anExpenseCategory().withId(5L).withName("Child").withParent(parent).build();

        when(service.findById(10L)).thenReturn(Optional.of(parent));
        when(service.findAll()).thenReturn(List.of(parent, child));

        mockMvc.perform(get("/v1/expense-categories/10").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.subCategories.length()").value(1))
                .andExpect(jsonPath("$.subCategories[0].id").value(5));

        verify(service, times(1)).findById(10L);
        verify(service, times(1)).findAll();
        verify(service, never()).parentWithChildrenIfChild(anyLong());
    }

    @Test
    public void getAll_shouldReturnParentsWithAndWithoutSubCategories() throws Exception {
        // Parent without children
        ExpenseCategory parentNoChild = ExpenseCategoryBuilder.anExpenseCategory().withId(10L).withName("NoChildParent").build();
        // Parent with one child
        ExpenseCategory parentWithChild = ExpenseCategoryBuilder.anExpenseCategory().withId(20L).withName("ParentWithChild").build();
        ExpenseCategory childOf20 = ExpenseCategoryBuilder.anExpenseCategory().withId(21L).withName("ChildOf20").withParent(parentWithChild).build();

        // Return all categories (parents then child) from service
        when(service.findAll()).thenReturn(List.of(parentNoChild, parentWithChild, childOf20));

        mockMvc.perform(get("/v1/expense-categories").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                // Expect two top-level parents
                .andExpect(jsonPath("$.length()").value(2))
                // First parent has no subCategories
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].subCategories.length()").value(0))
                // Second parent has one subCategory with id 21
                .andExpect(jsonPath("$[1].id").value(20))
                .andExpect(jsonPath("$[1].subCategories.length()").value(1))
                .andExpect(jsonPath("$[1].subCategories[0].id").value(21));

        verify(service, times(1)).findAll();
    }
}
