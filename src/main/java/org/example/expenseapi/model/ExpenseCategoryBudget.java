package org.example.expenseapi.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "expense_category_budgets",
       uniqueConstraints = @UniqueConstraint(columnNames = {"category_id", "year_col", "month_col"}))
public class ExpenseCategoryBudget extends BasicEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private ExpenseCategory category;

    // Use explicit column names to avoid reserved keyword conflicts in H2 (year/month)
    @Column(name = "year_col", nullable = true)
    private Integer year;

    @Column(name = "month_col", nullable = true)
    private Integer month; // 1..12

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal budget;

    @Column(name = "allow_overspend", nullable = false)
    private Boolean allowOverspend = Boolean.TRUE; // default to true now

    public ExpenseCategoryBudget() {
    }

    public ExpenseCategory getCategory() {
        return category;
    }

    public void setCategory(ExpenseCategory category) {
        this.category = category;
    }

    public int getYear() {
        return year == null ? 0 : year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getMonth() {
        return month == null ? 0 : month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public void setBudget(BigDecimal budget) {
        this.budget = budget;
    }

    public Boolean getAllowOverspend() {
        return allowOverspend == null ? Boolean.TRUE : allowOverspend;
    }

    public void setAllowOverspend(Boolean allowOverspend) {
        this.allowOverspend = allowOverspend;
    }
}
