package com.factura.demo.domain.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public final class Retencion extends ElectronicDocument {

    private final String fiscalPeriod; // MM/YYYY
    private final List<RetencionTax> retentionTaxes;

    public Retencion(
            String id,
            String emitterRuc,
            String emitterCompanyName,
            String emitterEstablishmentAddress,
            String establishmentCode,
            String emissionPointCode,
            String sequential,
            String obligationToKeepBooks,
            String environment,
            String contributorRegime,
            String specialContributorResolution,
            String retentionAgent,
            LocalDate emissionDate,
            Client client,
            String fiscalPeriod,
            List<RetencionTax> retentionTaxes
    ) {
        super(id, emitterRuc, emitterCompanyName, emitterEstablishmentAddress,
              establishmentCode, emissionPointCode, sequential, obligationToKeepBooks,
              environment, contributorRegime, specialContributorResolution, retentionAgent, emissionDate, client, 
              List.of(new InvoiceLine("RET", "RETENCION", java.math.BigDecimal.ONE, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO, List.of())), "01");
              
        this.fiscalPeriod = Objects.requireNonNull(fiscalPeriod, "Periodo fiscal es requerido");
        if (retentionTaxes == null || retentionTaxes.isEmpty()) {
            throw new IllegalArgumentException("El comprobante de retención debe contener al menos un impuesto retenido");
        }
        this.retentionTaxes = List.copyOf(retentionTaxes);
    }

    public String getFiscalPeriod() { return fiscalPeriod; }
    public List<RetencionTax> getRetentionTaxes() { return retentionTaxes; }
}
