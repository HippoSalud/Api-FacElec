package com.factura.demo.domain.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public final class NotaCredito extends ElectronicDocument {

    private final String modifiedDocumentType;
    private final String modifiedDocumentId;
    private final LocalDate modifiedDocumentEmissionDate;
    private final String modificationReason;

    public NotaCredito(
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
            String paymentMethod,
            String modifiedDocumentType,
            String modifiedDocumentId,
            LocalDate modifiedDocumentEmissionDate,
            String modificationReason
    ) {
        super(id, emitterRuc, emitterCompanyName, emitterEstablishmentAddress,
              establishmentCode, emissionPointCode, sequential, obligationToKeepBooks,
              environment, contributorRegime, specialContributorResolution, retentionAgent, emissionDate, client, lines, paymentMethod);
              
        this.modifiedDocumentType = Objects.requireNonNull(modifiedDocumentType, "Tipo de documento modificado es requerido");
        this.modifiedDocumentId = Objects.requireNonNull(modifiedDocumentId, "Número de documento modificado es requerido");
        this.modifiedDocumentEmissionDate = Objects.requireNonNull(modifiedDocumentEmissionDate, "Fecha de emisión del documento modificado es requerida");
        this.modificationReason = Objects.requireNonNull(modificationReason, "Motivo de la modificación es requerido");
    }

    public String getModifiedDocumentType() { return modifiedDocumentType; }
    public String getModifiedDocumentId() { return modifiedDocumentId; }
    public LocalDate getModifiedDocumentEmissionDate() { return modifiedDocumentEmissionDate; }
    public String getModificationReason() { return modificationReason; }
}
