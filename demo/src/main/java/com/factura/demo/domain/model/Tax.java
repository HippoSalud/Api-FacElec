package com.factura.demo.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Tax {
    private final String code; // e.g., "2" for IVA, "3" for ICE, "5" for IRBPNR
    private final String percentageCode; // e.g., "0" (0%), "2" (12%), "4" (15% new IVA), "6" (No objeto), "7" (Exento)
    private final BigDecimal rate; // e.g., 15.00
    private final BigDecimal taxableBase;
    private final BigDecimal value;

    public Tax(String code, String percentageCode, BigDecimal rate, BigDecimal taxableBase) {
        this.code = code;
        this.percentageCode = percentageCode;
        this.rate = rate;
        this.taxableBase = taxableBase;
        // Calculation: value = base * (rate / 100)
        this.value = taxableBase.multiply(rate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP))
                                .setScale(2, RoundingMode.HALF_UP);
    }

    public String getCode() {
        return code;
    }

    public String getPercentageCode() {
        return percentageCode;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public BigDecimal getTaxableBase() {
        return taxableBase;
    }

    public BigDecimal getValue() {
        return value;
    }
}
