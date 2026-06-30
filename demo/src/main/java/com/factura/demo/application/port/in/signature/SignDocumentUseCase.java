package com.factura.demo.application.port.in.signature;

import com.factura.demo.domain.model.signature.DocumentToSign;
import com.factura.demo.domain.model.signature.SignatureContext;

public interface SignDocumentUseCase {
    
    /**
     * Firma un documento utilizando el contexto de firma.
     * Orquesta la validación del certificado y la generación de la firma.
     * Se encarga de limpiar la contraseña de la memoria después del uso.
     * 
     * @param document El documento a firmar.
     * @param context El contexto de firma que contiene el P12.
     * @return Los bytes del documento firmado.
     */
    byte[] sign(DocumentToSign document, SignatureContext context);
}
