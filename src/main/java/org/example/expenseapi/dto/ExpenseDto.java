package org.example.expenseapi.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public class ExpenseDto {
    private Long id;
    private LocalDate expenseDate;
    private String designation;
    private Long expenseCategoryId;
    private Long expenseStatusId;
    // new: include full nested objects
    private ExpenseCategoryDto expenseCategory;
    private ExpenseStatusDto expenseStatus;
    private BigDecimal amount;

    // audit
    private Instant createdAt;
    private String createdBy;
    private Instant updatedAt;
    private String updatedBy;

    public ExpenseDto() {}

    // getters & setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getExpenseDate() { return expenseDate; }
    public void setExpenseDate(LocalDate expenseDate) { this.expenseDate = expenseDate; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public Long getExpenseCategoryId() { return expenseCategoryId; }
    public void setExpenseCategoryId(Long expenseCategoryId) { this.expenseCategoryId = expenseCategoryId; }

    public Long getExpenseStatusId() { return expenseStatusId; }
    public void setExpenseStatusId(Long expenseStatusId) { this.expenseStatusId = expenseStatusId; }

    public ExpenseCategoryDto getExpenseCategory() { return expenseCategory; }
    public void setExpenseCategory(ExpenseCategoryDto expenseCategory) { this.expenseCategory = expenseCategory; }

    public ExpenseStatusDto getExpenseStatus() { return expenseStatus; }
    public void setExpenseStatus(ExpenseStatusDto expenseStatus) { this.expenseStatus = expenseStatus; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
