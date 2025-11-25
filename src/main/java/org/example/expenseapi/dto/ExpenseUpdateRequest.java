package org.example.expenseapi.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ExpenseUpdateRequest {
    private LocalDate expenseDate;
    private String designation;
    private Long expenseCategoryId;
    private Long expenseStatusId;
    private BigDecimal amount;

    public ExpenseUpdateRequest() {}

    public LocalDate getExpenseDate() { return expenseDate; }
    public void setExpenseDate(LocalDate expenseDate) { this.expenseDate = expenseDate; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public Long getExpenseCategoryId() { return expenseCategoryId; }
    public void setExpenseCategoryId(Long expenseCategoryId) { this.expenseCategoryId = expenseCategoryId; }

    public Long getExpenseStatusId() { return expenseStatusId; }
    public void setExpenseStatusId(Long expenseStatusId) { this.expenseStatusId = expenseStatusId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}

