package com.factura.demo.application.port.in;

import com.factura.demo.domain.model.Invoice;

public interface ProcessInvoiceUseCase {
    
    /**
     * Executes the complete pipeline for an electronic invoice:
     * 1. Generate XML
     * 2. Sign XML (XAdES-BES)
     * 3. Submit to SRI Recepcion
     * 4. Query SRI Autorizacion
     * 5. Generate PDF RIDE
     *
     * @param invoice The invoice to process.
     * @return The updated invoice with authorization information and status.
     */
    Invoice process(Invoice invoice, byte[] p12Certificate, String p12Password);
}
