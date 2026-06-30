package com.factura.demo.application.port.in;

public interface DownloadPdfUseCase {
    
    enum DocumentType {
        INVOICE,
        NOTA_VENTA,
        PROFORMA,
        NOTA_CREDITO
    }

    /**
     * Retrieves the document and generates its PDF representation.
     *
     * @param documentId The ID of the document.
     * @param type The type of the document.
     * @return PDF byte array.
     */
    byte[] downloadPdf(String documentId, DocumentType type);
}
