package org.example.expenseapi.dto;

import org.example.expenseapi.model.ExpenseCategoryStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ExpenseCategoryDto {
    private Long id;
    private String name;
    private ExpenseCategoryStatus status;
    private Integer level;
    private List<ExpenseCategoryBudgetDto> budgets = new ArrayList<>();
    private Long parentId;
    private List<ExpenseCategoryDto> subCategories = new ArrayList<>();
    private BudgetStatus budgetStatus; // budget status for current month/year if a budget exists

    // audit
    private Instant createdAt;
    private String createdBy;
    private Instant updatedAt;
    private String updatedBy;

    public ExpenseCategoryDto() {}

    // getters & setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public ExpenseCategoryStatus getStatus() { return status; }
    public void setStatus(ExpenseCategoryStatus status) { this.status = status; }

    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }

    public List<ExpenseCategoryBudgetDto> getBudgets() { return budgets; }
    public void setBudgets(List<ExpenseCategoryBudgetDto> budgets) { this.budgets = budgets; }

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }

    public List<ExpenseCategoryDto> getSubCategories() { return subCategories; }
    public void setSubCategories(List<ExpenseCategoryDto> subCategories) { this.subCategories = subCategories; }

    public BudgetStatus getBudgetStatus() { return budgetStatus; }
    public void setBudgetStatus(BudgetStatus budgetStatus) { this.budgetStatus = budgetStatus; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
