package org.example.expenseapi.dto;

public class UserSettingsDto {
    private Long id;
    private Long userId;
    private String currency;
    private Integer decimalDigits;
    private String weekStart;
    private String currencySymbolPlacement;

    public UserSettingsDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public Integer getDecimalDigits() { return decimalDigits; }
    public void setDecimalDigits(Integer decimalDigits) { this.decimalDigits = decimalDigits; }

    public String getWeekStart() { return weekStart; }
    public void setWeekStart(String weekStart) { this.weekStart = weekStart; }

    public String getCurrencySymbolPlacement() { return currencySymbolPlacement; }
    public void setCurrencySymbolPlacement(String currencySymbolPlacement) { this.currencySymbolPlacement = currencySymbolPlacement; }
}
