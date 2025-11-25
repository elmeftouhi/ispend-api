package org.example.expenseapi.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

public class CurrencyFormatterTest {

    @Test
    public void validCurrencyBefore_usLocale() {
        String out = CurrencyFormatter.format(new BigDecimal("123.45"), "USD", 2, "BEFORE", Locale.US);
        assertEquals("$ 123.45", out);
    }

    @Test
    public void validCurrencyAfter_germanyLocale() {
        String out = CurrencyFormatter.format(new BigDecimal("123.45"), "EUR", 2, "AFTER", Locale.GERMANY);
        assertEquals("123,45 €", out);
    }

    @Test
    public void invalidCurrencyCode_fallsBackToCodeBefore() {
        String code = "INVALIDCODE";
        String out = CurrencyFormatter.format(new BigDecimal("123.45"), code, 2, "BEFORE", Locale.US);
        // when currency code is invalid the utility falls back to the raw code as symbol
        assertTrue(out.startsWith(code) || out.contains(code));
        assertTrue(out.contains("123.45"));
    }

    @Test
    public void nullLocale_doesNotThrow_and_returnsNonEmpty() {
        String out = CurrencyFormatter.format(new BigDecimal("12.3"), "USD", 2, "BEFORE", null);
        assertNotNull(out);
        assertFalse(out.isBlank());
    }

    @Test
    public void zeroAmount_isFormattedWithCurrencyAndDecimals() {
        String out = CurrencyFormatter.format(BigDecimal.ZERO, "USD", 2, "BEFORE", Locale.US);
        assertEquals("$ 0.00", out);
    }

    @Test
    public void nullDecimalDigits_defaultsToTwoFractions() {
        String out = CurrencyFormatter.format(new BigDecimal("123.4"), "USD", null, "BEFORE", Locale.US);
        assertEquals("$ 123.40", out);
    }

    @Test
    public void nullPlacement_usesLocaleCurrencyFormatter() {
        BigDecimal amount = new BigDecimal("123.45");
        String out = CurrencyFormatter.format(amount, "USD", 2, null, Locale.US);

        NumberFormat cf = NumberFormat.getCurrencyInstance(Locale.US);
        cf.setMinimumFractionDigits(2);
        cf.setMaximumFractionDigits(2);
        String expected = cf.format(amount);

        assertEquals(expected, out);
    }
}

