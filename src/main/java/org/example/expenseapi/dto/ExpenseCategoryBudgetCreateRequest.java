package org.example.expenseapi.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public class ExpenseCategoryBudgetCreateRequest {
    @NotNull(message = "year is required")
    @Min(2000)
    @Max(2100)
    private Integer year;

    @NotNull(message = "month is required")
    @Min(1)
    @Max(12)
    private Integer month;

    @NotNull(message = "budget is required")
    @PositiveOrZero(message = "budget must be zero or positive")
    private BigDecimal budget;

    // If true, creating expenses that exceed the budget will be allowed for this category/month
    private Boolean allowOverspend = Boolean.TRUE;

    public ExpenseCategoryBudgetCreateRequest() {}

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public Integer getMonth() { return month; }
    public void setMonth(Integer month) { this.month = month; }

    public BigDecimal getBudget() { return budget; }
    public void setBudget(BigDecimal budget) { this.budget = budget; }

    public Boolean getAllowOverspend() { return allowOverspend; }
    public void setAllowOverspend(Boolean allowOverspend) { this.allowOverspend = allowOverspend; }
}
