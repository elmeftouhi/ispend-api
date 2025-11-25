package org.example.expenseapi.dto;

import org.example.expenseapi.model.ExpenseCategoryStatus;

import java.util.List;

public class ExpenseCategoryUpdateRequest {
    private String name;
    private ExpenseCategoryStatus status;
    private Integer level;
    private Long parentId;
    // optional budgets updates (only allowOverspend is applied when provided)
    private List<ExpenseCategoryBudgetDto> budgets;

    public ExpenseCategoryUpdateRequest() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public ExpenseCategoryStatus getStatus() { return status; }
    public void setStatus(ExpenseCategoryStatus status) { this.status = status; }

    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }

    public List<ExpenseCategoryBudgetDto> getBudgets() { return budgets; }
    public void setBudgets(List<ExpenseCategoryBudgetDto> budgets) { this.budgets = budgets; }
}
