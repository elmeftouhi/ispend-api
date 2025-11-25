package org.example.expenseapi.dto;

import java.math.BigDecimal;

public class ExpenseCategoryBudgetDto {
    private Integer year;
    private Integer month;
    private BigDecimal budget;
    private Boolean allowOverspend;

    public ExpenseCategoryBudgetDto() {}

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public Integer getMonth() { return month; }
    public void setMonth(Integer month) { this.month = month; }

    public BigDecimal getBudget() { return budget; }
    public void setBudget(BigDecimal budget) { this.budget = budget; }

    public Boolean getAllowOverspend() { return allowOverspend; }
    public void setAllowOverspend(Boolean allowOverspend) { this.allowOverspend = allowOverspend; }
}
