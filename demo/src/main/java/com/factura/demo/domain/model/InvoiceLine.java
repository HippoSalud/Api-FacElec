package com.factura.demo.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;

public final class InvoiceLine {
    private final String mainCode;
    private final String description;
    private final BigDecimal quantity;
    private final BigDecimal unitPrice;
    private final BigDecimal discount;
    private final BigDecimal totalPriceWithoutTax;
    private final List<Tax> taxes;

    public InvoiceLine(String mainCode, String description, BigDecimal quantity, BigDecimal unitPrice, BigDecimal discount, List<Tax> taxes) {
        this.mainCode = mainCode;
        this.description = description;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.discount = discount == null ? BigDecimal.ZERO : discount;
        this.taxes = taxes == null ? Collections.emptyList() : List.copyOf(taxes);
        
        // Calculation: (quantity * unitPrice) - discount
        BigDecimal baseTotal = quantity.multiply(unitPrice);
        this.totalPriceWithoutTax = baseTotal.subtract(this.discount).setScale(2, RoundingMode.HALF_UP);
    }

    public String getMainCode() {
        return mainCode;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public BigDecimal getTotalPriceWithoutTax() {
        return totalPriceWithoutTax;
    }

    public List<Tax> getTaxes() {
        return taxes;
    }
}
