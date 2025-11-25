package org.example.expenseapi.dto;

/**
 * Update request for user settings. For weekStart provide a day name (e.g. MONDAY, Tuesday)
 */
public class UserSettingsUpdateRequest {
    private String currency;
    private Integer decimalDigits;
    private String weekStart; // day name
    private String currencySymbolPlacement; // BEFORE or AFTER

    public UserSettingsUpdateRequest() {}

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public Integer getDecimalDigits() { return decimalDigits; }
    public void setDecimalDigits(Integer decimalDigits) { this.decimalDigits = decimalDigits; }

    public String getWeekStart() { return weekStart; }
    public void setWeekStart(String weekStart) { this.weekStart = weekStart; }

    public String getCurrencySymbolPlacement() { return currencySymbolPlacement; }
    public void setCurrencySymbolPlacement(String currencySymbolPlacement) { this.currencySymbolPlacement = currencySymbolPlacement; }
}
