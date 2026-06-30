package com.factura.demo.domain.exception.signature;

public class InvalidKeystorePasswordException extends SignatureProcessingException {
    public InvalidKeystorePasswordException(String message) {
        super(message);
    }
    
    public InvalidKeystorePasswordException(String message, Throwable cause) {
        super(message, cause);
    }
}
