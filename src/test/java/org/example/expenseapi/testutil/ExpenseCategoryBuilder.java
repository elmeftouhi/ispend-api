package org.example.expenseapi.testutil;

import org.example.expenseapi.model.ExpenseCategory;
import org.example.expenseapi.model.ExpenseCategoryStatus;

import java.time.Instant;

public class ExpenseCategoryBuilder {
    private Long id;
    private String name = "Category";
    private ExpenseCategoryStatus status = ExpenseCategoryStatus.ACTIVE;
    private Integer level = 1;
    private ExpenseCategory parent;

    // audit
    private Instant createdAt;
    private String createdBy;
    private Instant updatedAt;
    private String updatedBy;

    public static ExpenseCategoryBuilder anExpenseCategory() {
        return new ExpenseCategoryBuilder();
    }

    // fluent aliases similar to UserBuilder
    public ExpenseCategoryBuilder withId(Long id) { this.id = id; return this; }
    public ExpenseCategoryBuilder withName(String name) { this.name = name; return this; }
    public ExpenseCategoryBuilder withStatus(ExpenseCategoryStatus status) { this.status = status; return this; }
    public ExpenseCategoryBuilder withLevel(Integer level) { this.level = level; return this; }
    public ExpenseCategoryBuilder withParent(ExpenseCategory parent) { this.parent = parent; return this; }

    // audit fluent aliases
    public ExpenseCategoryBuilder withCreatedAt(Instant createdAt) { this.createdAt = createdAt; return this; }
    public ExpenseCategoryBuilder withCreatedBy(String createdBy) { this.createdBy = createdBy; return this; }
    public ExpenseCategoryBuilder withUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }
    public ExpenseCategoryBuilder withUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; return this; }

    // short names
    public ExpenseCategoryBuilder id(Long id) { return withId(id); }
    public ExpenseCategoryBuilder name(String name) { return withName(name); }
    public ExpenseCategoryBuilder status(ExpenseCategoryStatus status) { return withStatus(status); }

    public ExpenseCategory build() {
        ExpenseCategory c = new ExpenseCategory();
        if (this.id != null) c.setId(this.id);
        c.setName(this.name);
        c.setStatus(this.status);
        c.setLevel(this.level);
        c.setParent(this.parent);
        if (this.createdAt != null) c.setCreatedAt(this.createdAt);
        if (this.createdBy != null) c.setCreatedBy(this.createdBy);
        if (this.updatedAt != null) c.setUpdatedAt(this.updatedAt);
        if (this.updatedBy != null) c.setUpdatedBy(this.updatedBy);
        return c;
    }
}
