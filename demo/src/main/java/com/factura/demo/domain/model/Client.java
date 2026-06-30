package com.factura.demo.domain.model;

import java.util.Objects;

public final class Client {
    private final String identificationType; // 04 = RUC, 05 = Cedula, 06 = Pasaporte, etc.
    private final String identification;
    private final String name;
    private final String address;
    private final String email;

    public Client(String identificationType, String identification, String name, String address, String email) {
        this.identificationType = Objects.requireNonNull(identificationType, "Tipo de identificación es requerido");
        this.identification = Objects.requireNonNull(identification, "Identificación es requerida");
        this.name = Objects.requireNonNull(name, "Razón social es requerida");
        this.address = address;
        this.email = email;
    }

    public String getIdentificationType() {
        return identificationType;
    }

    public String getIdentification() {
        return identification;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public String getEmail() {
        return email;
    }
}
