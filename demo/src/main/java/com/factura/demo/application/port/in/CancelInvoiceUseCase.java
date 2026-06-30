package com.factura.demo.application.port.in;

import com.factura.demo.domain.model.Invoice;

public interface CancelInvoiceUseCase {
    /**
     * Cancels an authorized invoice.
     *
     * @param invoiceId The ID of the invoice to cancel.
     * @param p12Certificate The P12 certificate as bytes, to sign the annulment request if needed.
     * @param p12Password The password for the P12 certificate.
     * @return The updated invoice.
     */
    Invoice cancel(String invoiceId, byte[] p12Certificate, String p12Password);
}
