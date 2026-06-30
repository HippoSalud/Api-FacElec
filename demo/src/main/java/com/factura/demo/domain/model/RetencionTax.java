package com.factura.demo.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public final class RetencionTax {
    private final String code; // 1=Renta, 2=IVA, 6=ISD
    private final String retentionCode; // e.g. "312", "343"
    private final BigDecimal taxableBase;
    private final BigDecimal retentionPercentage;
    private final BigDecimal retainedValue;
    private final String supportDocumentCode; // e.g. "01" (Factura)
    private final String supportDocumentId; // e.g. "001-001-000000001"
    private final LocalDate supportDocumentEmissionDate;

    public RetencionTax(
            String code,
            String retentionCode,
            BigDecimal taxableBase,
            BigDecimal retentionPercentage,
            BigDecimal retainedValue,
            String supportDocumentCode,
            String supportDocumentId,
            LocalDate supportDocumentEmissionDate
    ) {
        this.code = Objects.requireNonNull(code);
        this.retentionCode = Objects.requireNonNull(retentionCode);
        this.taxableBase = Objects.requireNonNull(taxableBase);
        this.retentionPercentage = Objects.requireNonNull(retentionPercentage);
        this.retainedValue = Objects.requireNonNull(retainedValue);
        this.supportDocumentCode = supportDocumentCode;
        this.supportDocumentId = supportDocumentId;
        this.supportDocumentEmissionDate = supportDocumentEmissionDate;
    }

    public String getCode() { return code; }
    public String getRetentionCode() { return retentionCode; }
    public BigDecimal getTaxableBase() { return taxableBase; }
    public BigDecimal getRetentionPercentage() { return retentionPercentage; }
    public BigDecimal getRetainedValue() { return retainedValue; }
    public String getSupportDocumentCode() { return supportDocumentCode; }
    public String getSupportDocumentId() { return supportDocumentId; }
    public LocalDate getSupportDocumentEmissionDate() { return supportDocumentEmissionDate; }
}
