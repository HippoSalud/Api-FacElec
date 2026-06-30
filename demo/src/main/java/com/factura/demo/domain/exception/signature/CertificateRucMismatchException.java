package com.factura.demo.domain.exception.signature;

public class CertificateRucMismatchException extends SignatureProcessingException {
    public CertificateRucMismatchException(String message) {
        super(message);
    }
}
