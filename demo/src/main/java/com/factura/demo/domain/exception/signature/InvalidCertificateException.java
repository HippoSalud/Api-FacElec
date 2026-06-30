package com.factura.demo.domain.exception.signature;

public class InvalidCertificateException extends SignatureProcessingException {
    public InvalidCertificateException(String message) {
        super(message);
    }
}
