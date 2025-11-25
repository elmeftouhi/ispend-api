package org.example.expenseapi.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

/**
 * Utility for formatting monetary amounts with currency symbol and placement.
 *
 * Usage examples:
 * - CurrencyFormatter.format(amount, "USD", 2, "BEFORE", Locale.US)
 * - CurrencyFormatter.format(amount, null, 2, null, Locale.getDefault()) // locale default
 *
 * The method is defensive: if currency code is invalid it falls back to locale currency or the code itself.
 */
public final class CurrencyFormatter {
    private CurrencyFormatter() { }

    /**
     * Format the given amount using provided settings.
     *
     * @param amount BigDecimal amount (may be null -> treated as zero)
     * @param currencyCode ISO 4217 code like "USD" or null to use locale currency
     * @param decimalDigits number of fraction digits to render (nullable -> 2)
     * @param placement "BEFORE" or "AFTER" (nullable -> use locale-based placement)
     * @param locale locale to use for number formatting (nullable -> Locale.getDefault())
     * @return formatted string containing currency symbol and formatted number
     */
    public static String format(BigDecimal amount, String currencyCode, Integer decimalDigits, String placement, Locale locale) {
        if (locale == null) locale = Locale.getDefault();
        BigDecimal v = amount == null ? BigDecimal.ZERO : amount;
        int frac = decimalDigits == null ? 2 : decimalDigits;

        // If no explicit placement, prefer locale-aware currency formatter
        if (placement == null) {
            try {
                NumberFormat cf = NumberFormat.getCurrencyInstance(locale);
                if (currencyCode != null && !currencyCode.isBlank()) {
                    try {
                        Currency c = Currency.getInstance(currencyCode.trim());
                        cf.setCurrency(c);
                    } catch (Exception ignored) {
                        // invalid currency code -> fall through to locale currency
                    }
                }
                cf.setMinimumFractionDigits(frac);
                cf.setMaximumFractionDigits(frac);
                return cf.format(v);
            } catch (Exception ex) {
                // fall through to manual composition
            }
        }

        // Build numeric part
        NumberFormat nf = NumberFormat.getNumberInstance(locale);
        nf.setMinimumFractionDigits(frac);
        nf.setMaximumFractionDigits(frac);
        String numberPart = nf.format(v);

        // Resolve symbol
        String symbol = "";
        try {
            if (currencyCode != null && !currencyCode.isBlank()) {
                try {
                    Currency c = Currency.getInstance(currencyCode.trim());
                    symbol = c.getSymbol(locale);
                } catch (Exception ex) {
                    // fallback to raw code
                    symbol = currencyCode.trim();
                }
            } else {
                Currency c = Currency.getInstance(locale);
                symbol = c.getSymbol(locale);
            }
        } catch (Exception ex) {
            if (currencyCode != null && !currencyCode.isBlank()) symbol = currencyCode.trim();
            else symbol = "";
        }

        if (placement != null && placement.equalsIgnoreCase("AFTER")) {
            return numberPart + (symbol.isEmpty() ? "" : " " + symbol);
        } else {
            // BEFORE or any other value -> default to before
            return (symbol.isEmpty() ? "" : symbol + " ") + numberPart;
        }
    }
}

