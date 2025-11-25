package org.example.expenseapi.dto;

import java.math.BigDecimal;
import java.util.Map;

public class YearlyExpenseDto {
    private Integer year;
    private BigDecimal total;
    private String totalFormated; // formatted total with currency symbol
    private Map<Integer, BigDecimal> months; // month(1-12) -> total

    public YearlyExpenseDto() {}

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public Map<Integer, BigDecimal> getMonths() { return months; }
    public void setMonths(Map<Integer, BigDecimal> months) { this.months = months; }

    public String getTotalFormated() { return totalFormated; }
    public void setTotalFormated(String totalFormated) { this.totalFormated = totalFormated; }
}
