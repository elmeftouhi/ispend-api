package org.example.expenseapi.dto;

import java.util.List;

public class ExpenseReportDto {

    private Long categoryId;
    private List<YearlyExpenseDto> expenses;

    public ExpenseReportDto() {
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public List<YearlyExpenseDto> getExpenses() {
        return expenses;
    }

    public void setExpenses(List<YearlyExpenseDto> expenses) {
        this.expenses = expenses;
    }
}

