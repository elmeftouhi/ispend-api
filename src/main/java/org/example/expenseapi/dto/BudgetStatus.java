package org.example.expenseapi.dto;

import java.math.BigDecimal;

public class BudgetStatus {
    private BigDecimal budget;
    private BigDecimal spent;
    private BigDecimal remaining;
    private boolean overBudget;
    private Boolean allowOverspend;

    public BigDecimal getBudget() { return budget; }
    public void setBudget(BigDecimal budget) { this.budget = budget; }
    public BigDecimal getSpent() { return spent; }
    public void setSpent(BigDecimal spent) { this.spent = spent; }
    public BigDecimal getRemaining() { return remaining; }
    public void setRemaining(BigDecimal remaining) { this.remaining = remaining; }
    public boolean isOverBudget() { return overBudget; }
    public void setOverBudget(boolean overBudget) { this.overBudget = overBudget; }
    public Boolean getAllowOverspend() { return allowOverspend; }
    public void setAllowOverspend(Boolean allowOverspend) { this.allowOverspend = allowOverspend; }
}
