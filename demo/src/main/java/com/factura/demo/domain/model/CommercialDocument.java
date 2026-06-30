package com.factura.demo.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public abstract class CommercialDocument {

    private final String id;
    private final String emitterRuc;
    private final String emitterCompanyName;
    private final String emitterEstablishmentAddress;
    private final String establishmentCode;
    private final String emissionPointCode;
    private final String sequential;
    private final LocalDate emissionDate;
    private final Client client;
    private final List<InvoiceLine> lines;
    private final String paymentMethod;
    
    protected Status status;

    public enum Status {
        CREADA,
        FIRMADA,
        RECIBIDA,
        AUTORIZADA,
        RECHAZADA,
        ANULADA // Nuevo estado
    }

    protected CommercialDocument(
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
        this.id = Objects.requireNonNull(id, "ID del documento es requerido");
        this.emitterRuc = Objects.requireNonNull(emitterRuc, "RUC del emisor es requerido");
        this.emitterCompanyName = Objects.requireNonNull(emitterCompanyName, "Nombre comercial del emisor es requerido");
        this.emitterEstablishmentAddress = emitterEstablishmentAddress;
        this.establishmentCode = Objects.requireNonNull(establishmentCode, "Código de establecimiento es requerido");
        this.emissionPointCode = Objects.requireNonNull(emissionPointCode, "Punto de emisión es requerido");
        this.sequential = Objects.requireNonNull(sequential, "Secuencial es requerido");
        this.emissionDate = Objects.requireNonNull(emissionDate, "Fecha de emisión es requerida");
        this.client = Objects.requireNonNull(client, "Cliente es requerido");
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("El documento debe contener al menos una línea de detalle");
        }
        this.lines = List.copyOf(lines);
        this.paymentMethod = paymentMethod == null ? "01" : paymentMethod;
        this.status = Status.CREADA;
    }

    public void markAsAnnulled() {
        if (this.status != Status.AUTORIZADA && this.status != Status.CREADA) {
            throw new IllegalStateException("Solo se pueden anular documentos en estado CREADA o AUTORIZADA");
        }
        this.status = Status.ANULADA;
    }

    public BigDecimal getTotalWithoutTax() {
        return lines.stream()
                .map(InvoiceLine::getTotalPriceWithoutTax)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getTotalDiscount() {
        return lines.stream()
                .map(InvoiceLine::getDiscount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getTotalTaxValue() {
        return lines.stream()
                .flatMap(line -> line.getTaxes().stream())
                .map(Tax::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getTotalAmount() {
        return getTotalWithoutTax().add(getTotalTaxValue()).setScale(2, RoundingMode.HALF_UP);
    }

    public String getId() { return id; }
    public String getEmitterRuc() { return emitterRuc; }
    public String getEmitterCompanyName() { return emitterCompanyName; }
    public String getEmitterEstablishmentAddress() { return emitterEstablishmentAddress; }
    public String getEstablishmentCode() { return establishmentCode; }
    public String getEmissionPointCode() { return emissionPointCode; }
    public String getSequential() { return sequential; }
    public LocalDate getEmissionDate() { return emissionDate; }
    public Client getClient() { return client; }
    public List<InvoiceLine> getLines() { return lines; }
    public String getPaymentMethod() { return paymentMethod; }
    public Status getStatus() { return status; }
}
