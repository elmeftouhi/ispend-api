package org.example.expenseapi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "expense_statuses")
public class ExpenseStatus extends BasicEntity {

    @Column(nullable = false, unique = true)
    private String name;

    // new column to mark default status (stored as boolean / 0 or 1 in DB)
    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    public ExpenseStatus() {
    }

    public ExpenseStatus(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }
}
