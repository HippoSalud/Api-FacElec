package com.factura.demo.domain.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Value Object representing the 49-digit Access Key (Clave de Acceso) for SRI Ecuador.
 */
public final class AccessKey {

    private final String value;

    private AccessKey(String value) {
        this.value = Objects.requireNonNull(value, "La clave de acceso no puede ser nula");
        if (value.length() != 49) {
            throw new IllegalArgumentException("La clave de acceso debe tener exactamente 49 caracteres");
        }
        if (!value.matches("\\d+")) {
            throw new IllegalArgumentException("La clave de acceso debe contener solo dígitos");
        }
    }

    public static AccessKey of(String value) {
        return new AccessKey(value);
    }

    /**
     * Generates a 49-digit Access Key based on parameters.
     */
    public static AccessKey generate(
            LocalDate emissionDate,
            String documentType,
            String ruc,
            String environment,
            String establishment,
            String emissionPoint,
            String sequential,
            String numericCode,
            String emissionType
    ) {
        validateInput(documentType, 2, "Tipo de comprobante");
        validateInput(ruc, 13, "RUC");
        validateInput(environment, 1, "Ambiente");
        validateInput(establishment, 3, "Establecimiento");
        validateInput(emissionPoint, 3, "Punto de emisión");
        validateInput(sequential, 9, "Secuencial");
        validateInput(numericCode, 8, "Código numérico");
        validateInput(emissionType, 1, "Tipo de emisión");

        String formattedDate = emissionDate.format(DateTimeFormatter.ofPattern("ddMMyyyy"));
        String series = establishment + emissionPoint;

        String base = formattedDate 
                    + documentType 
                    + ruc 
                    + environment 
                    + series 
                    + sequential 
                    + numericCode 
                    + emissionType;

        int checkDigit = calculateModulo11(base);
        return new AccessKey(base + checkDigit);
    }

    public String getValue() {
        return value;
    }

    private static void validateInput(String field, int expectedLength, String fieldName) {
        if (field == null || field.length() != expectedLength || !field.matches("\\d+")) {
            throw new IllegalArgumentException(String.format("%s inválido. Debe tener %d dígitos.", fieldName, expectedLength));
        }
    }

    /**
     * Calculates the Modulo 11 check digit as required by SRI Ecuador.
     */
    public static int calculateModulo11(String base) {
        int sum = 0;
        int factor = 2;
        
        // Loop from right to left
        for (int i = base.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(base.charAt(i));
            sum += digit * factor;
            factor++;
            if (factor > 7) {
                factor = 2;
            }
        }

        int remainder = sum % 11;
        int checkDigit = 11 - remainder;

        if (checkDigit == 11) {
            return 0;
        } else if (checkDigit == 10) {
            return 1;
        }
        return checkDigit;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AccessKey accessKey = (AccessKey) o;
        return value.equals(accessKey.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
