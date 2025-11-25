package org.example.expenseapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ExpenseCreateRequest {
    @NotNull(message = "expenseDate is required")
    private LocalDate expenseDate;

    @NotBlank(message = "designation is required")
    private String designation;

    @NotNull(message = "expenseCategoryId is required")
    private Long expenseCategoryId;

    // optional: when missing we'll use the configured default
    private Long expenseStatusId;

    @NotNull(message = "amount is required")
    @Positive(message = "amount must be greater than 0")
    private BigDecimal amount;

    public ExpenseCreateRequest() {}

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
