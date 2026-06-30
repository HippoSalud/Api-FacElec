package com.factura.demo.domain.exception.signature;

public class RevokedOrExpiredCertificateException extends SignatureProcessingException {
    public RevokedOrExpiredCertificateException(String message) {
        super(message);
    }
}
