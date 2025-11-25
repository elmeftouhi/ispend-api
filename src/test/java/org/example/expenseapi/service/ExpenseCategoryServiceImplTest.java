package org.example.expenseapi.service;

import org.example.expenseapi.model.ExpenseCategory;
import org.example.expenseapi.model.ExpenseCategoryStatus;
import org.example.expenseapi.repository.ExpenseCategoryRepository;
import org.example.expenseapi.service.impl.ExpenseCategoryServiceImpl;
import org.example.expenseapi.testutil.ExpenseCategoryBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpenseCategoryServiceImplTest {

    @Mock
    private ExpenseCategoryRepository repository;

    @InjectMocks
    private ExpenseCategoryServiceImpl service;

    private ExpenseCategory existing;

    @BeforeEach
    void setUp() {
        existing = ExpenseCategoryBuilder.anExpenseCategory()
                .withId(1L)
                .withName("Travel")
                .withStatus(ExpenseCategoryStatus.ACTIVE)
                .withLevel(1)
                .build();
    }

    @Test
    void createExpenseCategory_savesAndReturns() {
        ExpenseCategory toCreate = ExpenseCategoryBuilder.anExpenseCategory()
                .withName("Food")
                .withStatus(ExpenseCategoryStatus.ACTIVE)
                .withLevel(1)
                .build();

        when(repository.save(any(ExpenseCategory.class))).thenAnswer(invocation -> {
            ExpenseCategory arg = invocation.getArgument(0);
            arg.setId(2L);
            return arg;
        });

        ExpenseCategory created = service.createExpenseCategory(toCreate);

        assertNotNull(created);
        assertEquals(2L, created.getId());
        assertEquals("Food", created.getName());
        // budget is now stored in ExpenseCategoryBudget entities; no single-budget on category
        verify(repository).save(any(ExpenseCategory.class));
    }

    @Test
    void updateExpenseCategory_updatesFields() {
        ExpenseCategory updates = ExpenseCategoryBuilder.anExpenseCategory()
                .withName("Travel - Intl")
                .withLevel(2)
                .build();
        // status left null to ensure partial update works

        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any(ExpenseCategory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExpenseCategory updated = service.updateExpenseCategory(1L, updates);

        assertEquals("Travel - Intl", updated.getName());
        assertEquals(ExpenseCategoryStatus.ACTIVE, updated.getStatus()); // unchanged
        assertEquals(2, updated.getLevel());

        // budget unchanged here; budgets are managed via ExpenseCategoryBudget
        verify(repository).findById(1L);
        verify(repository).save(existing);
    }

    @Test
    void updateExpenseCategory_nonExisting_throws() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        ExpenseCategory updates = ExpenseCategoryBuilder.anExpenseCategory()
                .withName("Doesn't Matter")
                .build();

        assertThrows(RuntimeException.class, () -> service.updateExpenseCategory(99L, updates));
        verify(repository).findById(99L);
        verify(repository, never()).save(any());
    }

    @Test
    void deleteExpenseCategory_deletes() {
        doNothing().when(repository).deleteById(1L);

        assertDoesNotThrow(() -> service.deleteExpenseCategory(1L));
        verify(repository).deleteById(1L);
    }

    @Test
    void findById_and_findAll_and_findByName() {
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        // Service.findAll() now builds ordered list from parents and children; mock parent fetch and child fetch.
        when(repository.findByParentIsNullOrderByLevelAsc()).thenReturn(List.of(existing));
        when(repository.findByParentOrderByLevelAsc(existing)).thenReturn(List.of());
        when(repository.findByName("Travel")).thenReturn(Optional.of(existing));

        Optional<ExpenseCategory> byId = service.findById(1L);
        List<ExpenseCategory> all = service.findAll();
        Optional<ExpenseCategory> byName = service.findByName("Travel");

        assertTrue(byId.isPresent());
        assertEquals(existing.getName(), byId.get().getName());

        assertEquals(1, all.size());
        assertEquals(existing.getName(), all.get(0).getName());

        assertTrue(byName.isPresent());
        assertEquals(existing.getName(), byName.get().getName());

        verify(repository).findById(1L);
        verify(repository).findByParentIsNullOrderByLevelAsc();
        verify(repository).findByParentOrderByLevelAsc(existing);
        verify(repository).findByName("Travel");
    }

    // New tests for level management

    @Test
    void createWithoutLevel_placesAtBottom() {
        // existing siblings have levels 1 and 2
        ExpenseCategory s1 = ExpenseCategoryBuilder.anExpenseCategory().withId(10L).withLevel(1).withName("A").build();
        ExpenseCategory s2 = ExpenseCategoryBuilder.anExpenseCategory().withId(11L).withLevel(2).withName("B").build();

        when(repository.findByParentIsNullOrderByLevelAsc()).thenReturn(List.of(s1, s2));
        when(repository.save(any(ExpenseCategory.class))).thenAnswer(invocation -> {
            ExpenseCategory arg = invocation.getArgument(0);
            if (arg.getId() == null) arg.setId(20L);
            return arg;
        });

        ExpenseCategory newCat = ExpenseCategoryBuilder.anExpenseCategory().withName("NewCat").withLevel(null).build();

        ExpenseCategory created = service.createExpenseCategory(newCat);

        assertEquals(3, created.getLevel());
        verify(repository).findByParentIsNullOrderByLevelAsc();
        verify(repository).save(created);
    }

    @Test
    void createWithLevel_shiftsSiblings() {
        // siblings with levels 1,2,3
        ExpenseCategory s1 = ExpenseCategoryBuilder.anExpenseCategory().withId(10L).withLevel(1).withName("A").build();
        ExpenseCategory s2 = ExpenseCategoryBuilder.anExpenseCategory().withId(11L).withLevel(2).withName("B").build();
        ExpenseCategory s3 = ExpenseCategoryBuilder.anExpenseCategory().withId(12L).withLevel(3).withName("C").build();

        when(repository.findByParentIsNullOrderByLevelAsc()).thenReturn(List.of(s1, s2, s3));
        when(repository.save(any(ExpenseCategory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExpenseCategory newCat = ExpenseCategoryBuilder.anExpenseCategory().withName("Inserted").withLevel(2).build();

        ExpenseCategory created = service.createExpenseCategory(newCat);

        assertEquals(2, created.getLevel());

        // verify siblings with level >=2 were shifted up (B -> 3, C -> 4)
        ArgumentCaptor<ExpenseCategory> captor = ArgumentCaptor.forClass(ExpenseCategory.class);
        verify(repository, atLeast(3)).save(captor.capture());
        List<ExpenseCategory> saved = captor.getAllValues();

        boolean foundShiftedB = saved.stream().anyMatch(ec -> ec.getId()!=null && ec.getId().equals(11L) && ec.getLevel()==3);
        boolean foundShiftedC = saved.stream().anyMatch(ec -> ec.getId()!=null && ec.getId().equals(12L) && ec.getLevel()==4);
        assertTrue(foundShiftedB, "Sibling B should have been shifted to level 3");
        assertTrue(foundShiftedC, "Sibling C should have been shifted to level 4");
    }

    @Test
    void updateLevel_repositionsWithinSameParent() {
        // siblings: existing(id=1, level=1), s2(level=2), s3(level=3)
        ExpenseCategory s2 = ExpenseCategoryBuilder.anExpenseCategory().withId(11L).withLevel(2).withName("B").build();
        ExpenseCategory s3 = ExpenseCategoryBuilder.anExpenseCategory().withId(12L).withLevel(3).withName("C").build();

        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.findByParentIsNullOrderByLevelAsc()).thenReturn(List.of(existing, s2, s3));
        when(repository.save(any(ExpenseCategory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // move existing from level 1 to level 3
        ExpenseCategory updates = ExpenseCategoryBuilder.anExpenseCategory().withLevel(3).build();

        ExpenseCategory updated = service.updateExpenseCategory(1L, updates);

        assertEquals(3, updated.getLevel());

        // verify that s2 and s3 were adjusted: s2 should move to level 1? Actually service logic: when newLevel > oldLevel, decrement levels in (oldLevel+1..newLevel]
        // So s2(level2) becomes 1? Wait: oldLevel=1, newLevel=3: s with level>1 and <=3 have lvl-1 -> s2:2->1, s3:3->2
        ArgumentCaptor<ExpenseCategory> captor = ArgumentCaptor.forClass(ExpenseCategory.class);
        verify(repository, atLeast(2)).save(captor.capture());
        List<ExpenseCategory> saved = captor.getAllValues();

        boolean s2MovedTo1 = saved.stream().anyMatch(ec -> ec.getId()!=null && ec.getId().equals(11L) && ec.getLevel()==1);
        boolean s3MovedTo2 = saved.stream().anyMatch(ec -> ec.getId()!=null && ec.getId().equals(12L) && ec.getLevel()==2);
        assertTrue(s2MovedTo1, "Sibling s2 should have been decremented to level 1");
        assertTrue(s3MovedTo2, "Sibling s3 should have been decremented to level 2");
    }
}
