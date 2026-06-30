package com.factura.demo.domain.model;

import java.time.LocalDate;
import java.util.List;

public final class Proforma extends CommercialDocument {

    public Proforma(
            String id,
            String emitterRuc,
            String emitterCompanyName,
            String emitterEstablishmentAddress,
            String establishmentCode,
            String emissionPointCode,
            String sequential,
            LocalDate emissionDate,
            Client client,
            List<InvoiceLine> lines,
            String paymentMethod
    ) {
        super(id, emitterRuc, emitterCompanyName, emitterEstablishmentAddress,
              establishmentCode, emissionPointCode, sequential, emissionDate,
              client, lines, paymentMethod);
    }
}
