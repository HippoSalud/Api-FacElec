package com.factura.demo.domain.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public abstract class ElectronicDocument extends CommercialDocument {

    private final String obligationToKeepBooks;
    private final String environment;
    private final String contributorRegime; // e.g., "Contribuyente Negocio Popular - Régimen RIMPE"
    private final String specialContributorResolution; // e.g., "1308"
    private final String retentionAgent; // e.g., "1"

    private AccessKey accessKey;
    private String xmlContent;
    private String signedXmlContent;
    private String authorizationNumber;
    private LocalDate authorizationDate;

    protected ElectronicDocument(
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
              establishmentCode, emissionPointCode, sequential, emissionDate,
              client, lines, paymentMethod);
              
        this.obligationToKeepBooks = obligationToKeepBooks == null ? "NO" : obligationToKeepBooks;
        this.environment = Objects.requireNonNull(environment, "Ambiente es requerido");
        this.contributorRegime = contributorRegime;
        this.specialContributorResolution = specialContributorResolution;
        this.retentionAgent = retentionAgent;
    }

    public void associateAccessKey(AccessKey accessKey) {
        this.accessKey = Objects.requireNonNull(accessKey);
    }

    public void setXmlContent(String xmlContent) {
        this.xmlContent = xmlContent;
    }

    public void markAsSigned(String signedXmlContent) {
        this.signedXmlContent = Objects.requireNonNull(signedXmlContent);
        this.status = Status.FIRMADA;
    }

    public void markAsReceived() {
        this.status = Status.RECIBIDA;
    }

    public void markAsAuthorized(String authorizationNumber, LocalDate authorizationDate) {
        this.authorizationNumber = Objects.requireNonNull(authorizationNumber);
        this.authorizationDate = Objects.requireNonNull(authorizationDate);
        this.status = Status.AUTORIZADA;
    }

    public void markAsRejected() {
        this.status = Status.RECHAZADA;
    }

    public String getObligationToKeepBooks() { return obligationToKeepBooks; }
    public String getEnvironment() { return environment; }
    public AccessKey getAccessKey() { return accessKey; }
    public String getXmlContent() { return xmlContent; }
    public String getSignedXmlContent() { return signedXmlContent; }
    public String getAuthorizationNumber() { return authorizationNumber; }
    public LocalDate getAuthorizationDate() { return authorizationDate; }
    public String getContributorRegime() { return contributorRegime; }
    public String getSpecialContributorResolution() { return specialContributorResolution; }
    public String getRetentionAgent() { return retentionAgent; }
}
