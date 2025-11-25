package org.example.expenseapi.application.service;

import org.example.expenseapi.dto.ExpenseCategoryDto;
import org.example.expenseapi.model.ExpenseCategory;
import org.example.expenseapi.model.ExpenseCategoryStatus;
import org.example.expenseapi.service.ExpenseCategoryService;
import org.example.expenseapi.service.ExpenseBudgetService;
import org.example.expenseapi.testutil.ExpenseCategoryBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpenseCategoryApplicationServiceImplTest {

    @Mock
    private ExpenseCategoryService service;

    @Mock
    private ExpenseBudgetService budgetService;

    @InjectMocks
    private ExpenseCategoryApplicationServiceImpl appService;

    private ExpenseCategory parent;
    private ExpenseCategory child;

    @BeforeEach
    void setUp() {
        parent = ExpenseCategoryBuilder.anExpenseCategory()
                .withId(10L)
                .withName("Parent")
                .withStatus(ExpenseCategoryStatus.ACTIVE)
                .withLevel(1)
                .build();

        child = ExpenseCategoryBuilder.anExpenseCategory()
                .withId(5L)
                .withName("Child")
                .withParent(parent)
                .withStatus(ExpenseCategoryStatus.ACTIVE)
                .withLevel(2)
                .build();
    }

    @Test
    void create_delegatesToService() {
        ExpenseCategory toCreate = ExpenseCategoryBuilder.anExpenseCategory().withName("New").build();
        when(service.createExpenseCategory(toCreate)).thenReturn(toCreate);

        ExpenseCategory created = appService.create(toCreate);

        assertSame(toCreate, created);
        verify(service).createExpenseCategory(toCreate);
    }

    @Test
    void update_delegatesToService() {
        ExpenseCategory updates = ExpenseCategoryBuilder.anExpenseCategory().withName("Updated").build();
        when(service.updateExpenseCategory(1L, updates)).thenReturn(updates);

        ExpenseCategory out = appService.update(1L, updates);

        assertSame(updates, out);
        verify(service).updateExpenseCategory(1L, updates);
    }

    @Test
    void delete_delegatesToService() {
        doNothing().when(service).deleteExpenseCategory(2L);

        appService.delete(2L);

        verify(service).deleteExpenseCategory(2L);
    }

    @Test
    void find_methods_delegate() {
        when(service.findById(10L)).thenReturn(Optional.of(parent));
        when(service.findAll()).thenReturn(List.of(parent, child));
        when(service.findByName("Parent")).thenReturn(Optional.of(parent));

        Optional<ExpenseCategory> byId = appService.findById(10L);
        List<ExpenseCategory> all = appService.findAll();
        Optional<ExpenseCategory> byName = appService.findByName("Parent");

        assertTrue(byId.isPresent());
        assertEquals(parent.getName(), byId.get().getName());
        assertEquals(2, all.size());
        assertTrue(byName.isPresent());

        verify(service).findById(10L);
        verify(service).findAll();
        verify(service).findByName("Parent");
    }

    @Test
    void parentWithChildrenIfChild_whenChild_returnsParentDtoWithChildren() {
        when(service.findById(5L)).thenReturn(Optional.of(child));
        when(service.findAll()).thenReturn(List.of(parent, child));

        Optional<ExpenseCategoryDto> dtoOpt = appService.parentWithChildrenIfChild(5L);

        assertTrue(dtoOpt.isPresent());
        ExpenseCategoryDto dto = dtoOpt.get();
        assertEquals(parent.getId(), dto.getId());
        assertThat(dto.getSubCategories()).hasSize(1);
        assertEquals(child.getId(), dto.getSubCategories().get(0).getId());

        verify(service).findById(5L);
        verify(service).findAll();
    }

    @Test
    void parentWithChildrenIfChild_whenParentOrMissing_returnsEmpty() {
        // when id is parent (not child)
        when(service.findById(10L)).thenReturn(Optional.of(parent));
        Optional<ExpenseCategoryDto> dtoOpt = appService.parentWithChildrenIfChild(10L);
        assertTrue(dtoOpt.isEmpty());

        // when id not found
        when(service.findById(99L)).thenReturn(Optional.empty());
        Optional<ExpenseCategoryDto> missingOpt = appService.parentWithChildrenIfChild(99L);
        assertTrue(missingOpt.isEmpty());

        verify(service, times(2)).findById(anyLong());
    }

    @Test
    void siblingsForParent_whenParent_returnsOtherTopLevelParents() {
        ExpenseCategory other = ExpenseCategoryBuilder.anExpenseCategory().withId(11L).withName("Other").build();
        when(service.findById(10L)).thenReturn(Optional.of(parent));
        when(service.findAll()).thenReturn(List.of(parent, other, child));

        List<ExpenseCategoryDto> siblings = appService.siblingsForParent(10L);

        assertThat(siblings).hasSize(1);
        assertEquals(other.getId(), siblings.get(0).getId());

        verify(service).findById(10L);
        verify(service).findAll();
    }

    @Test
    void siblingsForParent_whenChildOrMissing_returnsEmpty() {
        when(service.findById(5L)).thenReturn(Optional.of(child));
        List<ExpenseCategoryDto> empty = appService.siblingsForParent(5L);
        assertThat(empty).isEmpty();

        when(service.findById(99L)).thenReturn(Optional.empty());
        List<ExpenseCategoryDto> empty2 = appService.siblingsForParent(99L);
        assertThat(empty2).isEmpty();

        verify(service, times(2)).findById(anyLong());
    }
}
