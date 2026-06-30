package com.factura.demo.application.port.out.signature;

import com.factura.demo.domain.model.signature.SignatureContext;

/**
 * Puerto de salida para validar certificados antes de la firma.
 * Cumple con los requisitos del SRI de Ecuador (entidades autorizadas, coincidencia de RUC, no revocado).
 */
public interface CertificateValidationPort {

    /**
     * Valida que el certificado en el contexto sea válido para emitir firmas en Ecuador.
     * Esto incluye:
     * 1. Verificar la contraseña (si falla, lanza InvalidKeystorePasswordException).
     * 2. Verificar que haya sido emitido por una Autoridad Certificadora válida en Ecuador.
     * 3. Verificar que el RUC esperado coincida con el RUC embebido en el certificado.
     * 4. Verificar que no esté caducado ni revocado.
     * 
     * @param context El contexto con el certificado a validar y el RUC esperado.
     */
    void validateCertificate(SignatureContext context);
}
