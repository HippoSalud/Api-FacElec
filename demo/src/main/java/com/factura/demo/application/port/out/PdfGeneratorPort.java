package com.factura.demo.application.port.out;

import com.factura.demo.domain.model.CommercialDocument;

public interface PdfGeneratorPort {
    /**
     * Generates a PDF representing the document.
     *
     * @param document The commercial document (Invoice, NotaVenta, Proforma).
     * @return Binary content (byte[]) of the generated PDF.
     */
    byte[] generateRide(CommercialDocument document);
}
