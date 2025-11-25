package org.example.expenseapi.application.service;

import org.example.expenseapi.dto.ExpenseCategoryDto;
import org.example.expenseapi.model.ExpenseCategory;
import org.example.expenseapi.service.ExpenseCategoryService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ExpenseCategoryApplicationServiceImpl implements ExpenseCategoryApplicationService {

    private final ExpenseCategoryService service;
    private final org.example.expenseapi.service.ExpenseBudgetService budgetService;

    public ExpenseCategoryApplicationServiceImpl(ExpenseCategoryService service, org.example.expenseapi.service.ExpenseBudgetService budgetService) {
        this.service = service;
        this.budgetService = budgetService;
    }

    @Override
    public ExpenseCategory create(ExpenseCategory category) {
        return service.createExpenseCategory(category);
    }

    @Override
    public ExpenseCategory update(Long id, ExpenseCategory category) {
        return service.updateExpenseCategory(id, category);
    }

    @Override
    public void delete(Long id) {
        service.deleteExpenseCategory(id);
    }

    @Override
    public Optional<ExpenseCategory> findById(Long id) {
        return service.findById(id);
    }

    @Override
    public List<ExpenseCategory> findAll() {
        return service.findAll();
    }

    @Override
    public Optional<ExpenseCategory> findByName(String name) {
        return service.findByName(name);
    }

    @Override
    public Optional<ExpenseCategoryDto> parentWithChildrenIfChild(Long id) {
        Optional<ExpenseCategory> opt = service.findById(id);
        if (opt.isEmpty()) return Optional.empty();
        ExpenseCategory current = opt.get();
        if (current.getParent() == null) return Optional.empty();
        ExpenseCategory parent = current.getParent();

        ExpenseCategoryDto parentDto = toDto(parent, true);
        return Optional.of(parentDto);
    }

    @Override
    public List<ExpenseCategoryDto> siblingsForParent(Long id) {
        Optional<ExpenseCategory> opt = service.findById(id);
        if (opt.isEmpty()) return List.of();
        ExpenseCategory current = opt.get();
        if (current.getParent() != null) return List.of();

        List<ExpenseCategory> all = service.findAll();
        return all.stream()
                .filter(c -> c.getParent() == null && !c.getId().equals(current.getId()))
                .map(c -> toDto(c, false))
                .collect(Collectors.toList());
    }

    // mapping helper (shallow unless includeChildren=true)
    private ExpenseCategoryDto toDto(ExpenseCategory c, boolean includeChildren) {
        ExpenseCategoryDto dto = new ExpenseCategoryDto();
        dto.setId(c.getId());
        dto.setName(c.getName());
        dto.setStatus(c.getStatus());
        dto.setLevel(c.getLevel());
        dto.setParentId(c.getParent() != null ? c.getParent().getId() : null);
        dto.setCreatedAt(c.getCreatedAt());
        dto.setCreatedBy(c.getCreatedBy());
        dto.setUpdatedAt(c.getUpdatedAt());
        dto.setUpdatedBy(c.getUpdatedBy());

        // populate budgets
        var budgets = budgetService.findBudgetsForCategory(c.getId());
        java.util.List<org.example.expenseapi.dto.ExpenseCategoryBudgetDto> bdto = new java.util.ArrayList<>();
        for (var b : budgets) {
            var eb = new org.example.expenseapi.dto.ExpenseCategoryBudgetDto();
            eb.setYear(b.getYear()); eb.setMonth(b.getMonth()); eb.setBudget(b.getBudget());
            bdto.add(eb);
        }
        dto.setBudgets(bdto);

        if (includeChildren) {
            List<ExpenseCategory> all = service.findAll();
            List<ExpenseCategoryDto> children = all.stream()
                    .filter(ch -> ch.getParent() != null && ch.getParent().getId() != null && ch.getParent().getId().equals(c.getId()))
                    .map(ch -> toDto(ch, false))
                    .collect(Collectors.toList());
            dto.setSubCategories(children);
        }

        return dto;
    }
}
