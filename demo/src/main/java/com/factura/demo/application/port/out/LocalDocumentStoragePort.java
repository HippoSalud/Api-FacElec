package com.factura.demo.application.port.out;

public interface LocalDocumentStoragePort {
    /**
     * Saves the XML document physically on the file system.
     *
     * @param documentId The unique ID of the document (used for the filename).
     * @param xmlContent The signed and authorized XML content.
     * @param documentType Type of document (e.g., "factura", "notaventa").
     * @return The absolute path where the file was saved.
     */
    String saveXml(String documentId, String xmlContent, String documentType);
}
