package org.example.expenseapi.service.impl;

import org.example.expenseapi.model.ExpenseCategory;
import org.example.expenseapi.model.ExpenseCategoryStatus;
import org.example.expenseapi.repository.ExpenseCategoryRepository;
import org.example.expenseapi.service.ExpenseCategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ExpenseCategoryServiceImpl implements ExpenseCategoryService {

    private final ExpenseCategoryRepository repository;

    public ExpenseCategoryServiceImpl(ExpenseCategoryRepository repository) {
        this.repository = repository;
    }

    @Override
    public ExpenseCategory createExpenseCategory(ExpenseCategory category) {
        // Determine sibling list based on parent (null parent -> top-level)
        List<ExpenseCategory> siblings = (category.getParent() == null)
                ? repository.findByParentIsNullOrderByLevelAsc()
                : repository.findByParentOrderByLevelAsc(category.getParent());

        Integer requestedLevel = category.getLevel();
        if (requestedLevel == null || requestedLevel <= 0) {
            // place at the bottom
            int next = 1;
            if (!siblings.isEmpty()) {
                ExpenseCategory last = siblings.get(siblings.size() - 1);
                if (last.getLevel() != null) next = last.getLevel() + 1;
            }
            category.setLevel(next);
        } else {
            // ensure at least 1 (requestedLevel > 0 here)
            int newLevel = requestedLevel;
            // shift siblings with level >= newLevel
            for (ExpenseCategory s : siblings) {
                Integer lvl = s.getLevel();
                if (lvl != null && lvl >= newLevel) {
                    s.setLevel(lvl + 1);
                    repository.save(s);
                }
            }
            category.setLevel(newLevel);
        }

        if (category.getStatus() == null) {
            category.setStatus(ExpenseCategoryStatus.ACTIVE);
        }

        return repository.save(category);
    }

    @Override
    public ExpenseCategory updateExpenseCategory(Long id, ExpenseCategory category) {
        ExpenseCategory existing = repository.findById(id).orElseThrow(() -> new RuntimeException("ExpenseCategory not found: " + id));

        // Track previous parent and level
        ExpenseCategory oldParent = existing.getParent();
        Integer oldLevel = existing.getLevel();

        if (category.getName() != null) existing.setName(category.getName());
        if (category.getStatus() != null) existing.setStatus(category.getStatus());
        // We'll handle level and parent specially below
        // if (category.getBudget() != null) existing.setBudget(category.getBudget());
        // budget is managed via ExpenseCategoryBudget entities; ignore single-budget updates here

        ExpenseCategory newParent = (category.getParent() != null) ? category.getParent() : existing.getParent();
        Integer requestedLevel = category.getLevel();

        // If parent changed, remove gap from old siblings
        if ((oldParent == null && newParent != null) || (oldParent != null && !oldParent.equals(newParent))) {
            // decrement levels of siblings in old parent that were greater than oldLevel
            List<ExpenseCategory> oldSiblings = (oldParent == null)
                    ? repository.findByParentIsNullOrderByLevelAsc()
                    : repository.findByParentOrderByLevelAsc(oldParent);
            if (oldLevel != null) {
                for (ExpenseCategory s : oldSiblings) {
                    Integer lvl = s.getLevel();
                    if (lvl != null && lvl > oldLevel) {
                        s.setLevel(lvl - 1);
                        repository.save(s);
                    }
                }
            }
        }

        // Determine siblings for the new parent (could be same as old parent)
        List<ExpenseCategory> newSiblings = (newParent == null)
                ? repository.findByParentIsNullOrderByLevelAsc()
                : repository.findByParentOrderByLevelAsc(newParent);

        // If level requested, place there, shifting others as needed
        if (requestedLevel != null && requestedLevel > 0) {
            int newLevel = requestedLevel;
            // If moving within same sibling list and level increased or decreased, handle accordingly
            if ((oldParent == null && newParent == null) || (oldParent != null && oldParent.equals(newParent))) {
                // same sibling list
                if (oldLevel == null) oldLevel = Integer.MAX_VALUE; // safety
                if (newLevel > oldLevel) {
                    // shifting others down between oldLevel+1 .. newLevel => decrement by 1
                    for (ExpenseCategory s : newSiblings) {
                        Integer lvl = s.getLevel();
                        if (lvl != null && lvl > oldLevel && lvl <= newLevel) {
                            s.setLevel(lvl - 1);
                            repository.save(s);
                        }
                    }
                } else if (newLevel < oldLevel) {
                    // shifting others up between newLevel .. oldLevel-1 => increment by 1
                    for (ExpenseCategory s : newSiblings) {
                        Integer lvl = s.getLevel();
                        if (lvl != null && lvl >= newLevel && lvl < oldLevel) {
                            s.setLevel(lvl + 1);
                            repository.save(s);
                        }
                    }
                }
                existing.setLevel(newLevel);
            } else {
                // moved to different parent (we already removed gap from old siblings)
                // shift new siblings >= newLevel up by 1
                for (ExpenseCategory s : newSiblings) {
                    Integer lvl = s.getLevel();
                    if (lvl != null && lvl >= newLevel) {
                        s.setLevel(lvl + 1);
                        repository.save(s);
                    }
                }
                existing.setLevel(newLevel);
            }
        } else {
            // no level requested: if parent changed, put at bottom of new siblings; otherwise keep existing level
            if ((oldParent == null && newParent != null) || (oldParent != null && !oldParent.equals(newParent))) {
                int next = 1;
                if (!newSiblings.isEmpty()) {
                    ExpenseCategory last = newSiblings.get(newSiblings.size() - 1);
                    if (last.getLevel() != null) next = last.getLevel() + 1;
                }
                existing.setLevel(next);
            }
            // else keep existing.level as-is
        }

        // Finally set the parent if changed
        existing.setParent(newParent);

        return repository.save(existing);
    }

    @Override
    public void deleteExpenseCategory(Long id) {
        repository.deleteById(id);
    }

    @Override
    public Optional<ExpenseCategory> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public List<ExpenseCategory> findAll() {
        // Return parents ordered by level, and for each parent append its children ordered by level.
        List<ExpenseCategory> ordered = new ArrayList<>();
        List<ExpenseCategory> parents = repository.findByParentIsNullOrderByLevelAsc();
        for (ExpenseCategory parent : parents) {
            ordered.add(parent);
            List<ExpenseCategory> children = repository.findByParentOrderByLevelAsc(parent);
            if (children != null && !children.isEmpty()) {
                ordered.addAll(children);
            }
        }
        return ordered;
    }

    @Override
    public Optional<ExpenseCategory> findByName(String name) {
        return repository.findByName(name);
    }
}
