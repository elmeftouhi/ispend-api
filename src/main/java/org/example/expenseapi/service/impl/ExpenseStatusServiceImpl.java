package org.example.expenseapi.service.impl;

import org.example.expenseapi.model.ExpenseStatus;
import org.example.expenseapi.repository.ExpenseStatusRepository;
import org.example.expenseapi.service.ExpenseStatusService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ExpenseStatusServiceImpl implements ExpenseStatusService {

    private final ExpenseStatusRepository repository;

    public ExpenseStatusServiceImpl(ExpenseStatusRepository repository) {
        this.repository = repository;
    }

    @Override
    public ExpenseStatus createExpenseStatus(ExpenseStatus status) {
        // ensure isDefault is not null
        if (status.getIsDefault() == null) status.setIsDefault(false);
        ExpenseStatus saved = repository.save(status);
        // if this one is now default, clear others
        if (Boolean.TRUE.equals(saved.getIsDefault())) {
            repository.clearOtherDefaults(saved.getId());
        }
        return saved;
    }

    @Override
    public ExpenseStatus updateExpenseStatus(Long id, ExpenseStatus status) {
        ExpenseStatus existing = repository.findById(id).orElseThrow(() -> new RuntimeException("ExpenseStatus not found: " + id));
        if (status.getName() != null) existing.setName(status.getName());
        if (status.getIsDefault() != null) existing.setIsDefault(status.getIsDefault());
        ExpenseStatus saved = repository.save(existing);
        if (Boolean.TRUE.equals(saved.getIsDefault())) {
            repository.clearOtherDefaults(saved.getId());
        }
        return saved;
    }

    @Override
    public void deleteExpenseStatus(Long id) {
        repository.deleteById(id);
    }

    @Override
    public Optional<ExpenseStatus> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public List<ExpenseStatus> findAll() {
        return repository.findAll();
    }

    @Override
    public Optional<ExpenseStatus> findByName(String name) {
        return repository.findByName(name);
    }

    @Override
    public Optional<ExpenseStatus> findDefaultStatus() {
        return repository.findByIsDefaultTrue();
    }
}
