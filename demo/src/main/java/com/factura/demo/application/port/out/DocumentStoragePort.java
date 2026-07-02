package com.factura.demo.application.port.out;

public interface DocumentStoragePort {
    /**
     * Saves the XML document.
     *
     * @param documentId The unique ID of the document.
     * @param xmlContent The signed and authorized XML content.
     * @param documentType Type of document (e.g., "factura", "notaventa").
     * @return The URL or path where the file was saved.
     */
    String saveXml(String documentId, String xmlContent, String documentType);
}
