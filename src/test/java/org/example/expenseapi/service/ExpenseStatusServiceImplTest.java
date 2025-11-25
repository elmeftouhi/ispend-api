package org.example.expenseapi.service;

import org.example.expenseapi.model.ExpenseStatus;
import org.example.expenseapi.repository.ExpenseStatusRepository;
import org.example.expenseapi.service.impl.ExpenseStatusServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpenseStatusServiceImplTest {

    @Mock
    private ExpenseStatusRepository repository;

    @InjectMocks
    private ExpenseStatusServiceImpl service;

    private ExpenseStatus existing;

    @BeforeEach
    void setUp() {
        existing = new ExpenseStatus();
        existing.setId(1L);
        existing.setName("Existing");
    }

    @Test
    void createExpenseStatus_savesAndReturns() {
        ExpenseStatus toCreate = new ExpenseStatus();
        toCreate.setName("NewStatus");

        when(repository.save(any(ExpenseStatus.class))).thenAnswer(invocation -> {
            ExpenseStatus arg = invocation.getArgument(0);
            arg.setId(2L);
            return arg;
        });

        ExpenseStatus created = service.createExpenseStatus(toCreate);

        assertNotNull(created);
        assertEquals(2L, created.getId());
        assertEquals("NewStatus", created.getName());
        verify(repository).save(any(ExpenseStatus.class));
    }

    @Test
    void updateExpenseStatus_updatesName() {
        ExpenseStatus updates = new ExpenseStatus();
        updates.setName("UpdatedName");

        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any(ExpenseStatus.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExpenseStatus updated = service.updateExpenseStatus(1L, updates);

        assertEquals("UpdatedName", updated.getName());
        verify(repository).findById(1L);
        verify(repository).save(existing);
    }

    @Test
    void updateExpenseStatus_nonExisting_throws() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        ExpenseStatus updates = new ExpenseStatus();
        updates.setName("Nope");

        assertThrows(RuntimeException.class, () -> service.updateExpenseStatus(99L, updates));
        verify(repository).findById(99L);
        verify(repository, never()).save(any());
    }

    @Test
    void deleteExpenseStatus_deletes() {
        doNothing().when(repository).deleteById(1L);

        assertDoesNotThrow(() -> service.deleteExpenseStatus(1L));
        verify(repository).deleteById(1L);
    }

    @Test
    void findById_and_findAll_and_findByName() {
        ExpenseStatus a = new ExpenseStatus();
        a.setId(10L);
        a.setName("A");
        ExpenseStatus b = new ExpenseStatus();
        b.setId(11L);
        b.setName("B");

        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.findAll()).thenReturn(List.of(a, b));
        when(repository.findByName("Existing")).thenReturn(Optional.of(existing));

        Optional<ExpenseStatus> byId = service.findById(1L);
        List<ExpenseStatus> all = service.findAll();
        Optional<ExpenseStatus> byName = service.findByName("Existing");

        assertTrue(byId.isPresent());
        assertEquals(existing.getName(), byId.get().getName());

        assertEquals(2, all.size());

        assertTrue(byName.isPresent());
        assertEquals(existing.getName(), byName.get().getName());

        verify(repository).findById(1L);
        verify(repository).findAll();
        verify(repository).findByName("Existing");
    }

    // New tests for isDefault behavior

    @Test
    void createExpenseStatus_whenIsDefaultTrue_clearsOthers() {
        ExpenseStatus toCreate = new ExpenseStatus();
        toCreate.setName("NewDefault");
        toCreate.setIsDefault(true);

        when(repository.save(any(ExpenseStatus.class))).thenAnswer(invocation -> {
            ExpenseStatus arg = invocation.getArgument(0);
            arg.setId(2L);
            return arg;
        });
        when(repository.clearOtherDefaults(2L)).thenReturn(1);

        ExpenseStatus created = service.createExpenseStatus(toCreate);

        assertTrue(Boolean.TRUE.equals(created.getIsDefault()));
        verify(repository).save(any(ExpenseStatus.class));
        verify(repository).clearOtherDefaults(2L);
    }

    @Test
    void createExpenseStatus_whenIsDefaultFalse_doesNotClearOthers() {
        ExpenseStatus toCreate = new ExpenseStatus();
        toCreate.setName("NewNonDefault");
        toCreate.setIsDefault(false);

        when(repository.save(any(ExpenseStatus.class))).thenAnswer(invocation -> {
            ExpenseStatus arg = invocation.getArgument(0);
            arg.setId(3L);
            return arg;
        });

        ExpenseStatus created = service.createExpenseStatus(toCreate);

        assertFalse(Boolean.TRUE.equals(created.getIsDefault()));
        verify(repository).save(any(ExpenseStatus.class));
        verify(repository, never()).clearOtherDefaults(anyLong());
    }

    @Test
    void updateExpenseStatus_whenSetToDefaultTrue_clearsOthers() {
        ExpenseStatus updates = new ExpenseStatus();
        updates.setIsDefault(true);

        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any(ExpenseStatus.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.clearOtherDefaults(1L)).thenReturn(1);

        ExpenseStatus saved = service.updateExpenseStatus(1L, updates);

        assertTrue(Boolean.TRUE.equals(saved.getIsDefault()));
        verify(repository).findById(1L);
        verify(repository).save(existing);
        verify(repository).clearOtherDefaults(1L);
    }

    @Test
    void updateExpenseStatus_whenSetToDefaultFalse_doesNotClearOthers() {
        ExpenseStatus updates = new ExpenseStatus();
        updates.setIsDefault(false);

        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any(ExpenseStatus.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExpenseStatus saved = service.updateExpenseStatus(1L, updates);

        assertFalse(Boolean.TRUE.equals(saved.getIsDefault()));
        verify(repository).findById(1L);
        verify(repository).save(existing);
        verify(repository, never()).clearOtherDefaults(anyLong());
    }
}
