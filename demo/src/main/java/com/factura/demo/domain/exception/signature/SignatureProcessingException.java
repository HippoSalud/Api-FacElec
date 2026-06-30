package com.factura.demo.domain.exception.signature;

public class SignatureProcessingException extends RuntimeException {
    public SignatureProcessingException(String message) {
        super(message);
    }

    public SignatureProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
