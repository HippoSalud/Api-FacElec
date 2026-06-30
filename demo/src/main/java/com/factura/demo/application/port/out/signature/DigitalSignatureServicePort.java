package com.factura.demo.application.port.out.signature;

import com.factura.demo.domain.model.signature.DocumentToSign;
import com.factura.demo.domain.model.signature.SignatureContext;
import com.factura.demo.domain.model.signature.SignatureProfile;

/**
 * Puerto de salida para realizar firmas digitales sobre documentos (XML o PDF).
 */
public interface DigitalSignatureServicePort {
    
    /**
     * Firma un documento utilizando el contexto de firma (que contiene el certificado y clave)
     * y el perfil de firma.
     * 
     * @param document El documento a firmar (XML o PDF).
     * @param context El contexto de firma (P12 y credenciales).
     * @param profile El perfil de firma que define el algoritmo y estándar.
     * @return El documento firmado en bytes.
     */
    byte[] signDocument(DocumentToSign document, SignatureContext context, SignatureProfile profile);
}
