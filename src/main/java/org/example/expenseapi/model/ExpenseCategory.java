package org.example.expenseapi.model;

import jakarta.persistence.*;

@Entity
@Table(name = "expense_categories")
public class ExpenseCategory extends BasicEntity {

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExpenseCategoryStatus status;

    @Column(nullable = true)
    private Integer level;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_expense_category_parent")
    private ExpenseCategory parent;

    public ExpenseCategory() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ExpenseCategoryStatus getStatus() {
        return status;
    }

    public void setStatus(ExpenseCategoryStatus status) {
        this.status = status;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public ExpenseCategory getParent() {
        return parent;
    }

    public void setParent(ExpenseCategory parent) {
        this.parent = parent;
    }
}
