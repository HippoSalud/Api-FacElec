package com.factura.demo.domain.model;

import java.time.LocalDate;
import java.util.List;

public final class Invoice extends ElectronicDocument {

    public Invoice(
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
            List<InvoiceLine> lines,
            String paymentMethod
    ) {
        super(id, emitterRuc, emitterCompanyName, emitterEstablishmentAddress,
              establishmentCode, emissionPointCode, sequential, obligationToKeepBooks,
              environment, contributorRegime, specialContributorResolution, retentionAgent, emissionDate, client, lines, paymentMethod);
    }
}
