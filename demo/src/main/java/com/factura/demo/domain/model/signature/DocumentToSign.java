package com.factura.demo.domain.model.signature;

import lombok.Builder;
import lombok.Getter;

/**
 * Representa un documento (XML o PDF) que necesita ser firmado digitalmente.
 */
@Getter
@Builder
public class DocumentToSign {
    private final byte[] content;
    private final DocumentType type;
    
    // Opcional, nombre lógico del archivo o ID interno
    private final String documentId;

    public enum DocumentType {
        XML,
        PDF
    }
}
