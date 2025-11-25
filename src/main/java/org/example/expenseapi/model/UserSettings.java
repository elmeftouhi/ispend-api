package org.example.expenseapi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_settings")
public class UserSettings extends BasicEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_user", nullable = false, unique = true)
    private User user;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Column(name = "decimal_digits", nullable = false)
    private Integer decimalDigits;

    @Column(name = "week_start", nullable = false)
    private String weekStart; // store day name, e.g. MONDAY

    @Column(name = "currency_symbol_placement", nullable = true)
    private String currencySymbolPlacement; // "BEFORE" or "AFTER"

    public UserSettings() {
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Integer getDecimalDigits() {
        return decimalDigits;
    }

    public void setDecimalDigits(Integer decimalDigits) {
        this.decimalDigits = decimalDigits;
    }

    public String getWeekStart() {
        return weekStart;
    }

    public void setWeekStart(String weekStart) {
        this.weekStart = weekStart;
    }

    public String getCurrencySymbolPlacement() {
        return currencySymbolPlacement;
    }

    public void setCurrencySymbolPlacement(String currencySymbolPlacement) {
        this.currencySymbolPlacement = currencySymbolPlacement;
    }
}
