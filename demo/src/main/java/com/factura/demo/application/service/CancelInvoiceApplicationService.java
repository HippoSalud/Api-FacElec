package com.factura.demo.application.service;

import com.factura.demo.application.port.in.CancelInvoiceUseCase;
import com.factura.demo.application.port.out.InvoiceRepositoryPort;
import com.factura.demo.application.port.out.SignaturePort;
import com.factura.demo.application.port.out.SriGatewayPort;
import com.factura.demo.domain.model.CommercialDocument;
import com.factura.demo.domain.model.Invoice;

import java.util.Objects;

public class CancelInvoiceApplicationService implements CancelInvoiceUseCase {

    private final InvoiceRepositoryPort invoiceRepository;
    private final SignaturePort signaturePort;
    private final SriGatewayPort sriGateway;

    public CancelInvoiceApplicationService(
            InvoiceRepositoryPort invoiceRepository,
            SignaturePort signaturePort,
            SriGatewayPort sriGateway
    ) {
        this.invoiceRepository = Objects.requireNonNull(invoiceRepository);
        this.signaturePort = Objects.requireNonNull(signaturePort);
        this.sriGateway = Objects.requireNonNull(sriGateway);
    }

    @Override
    public Invoice cancel(String invoiceId, byte[] p12Certificate, String p12Password) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Factura no encontrada"));

        if (invoice.getStatus() != CommercialDocument.Status.AUTORIZADA) {
            throw new IllegalStateException("Solo se pueden anular facturas en estado AUTORIZADA en el SRI");
        }

        // 1. Prepare XML for annulment
        String annulmentXml = generateAnnulmentXml(invoice);

        // 2. Sign annulment XML
        String signedAnnulmentXml = signaturePort.sign(annulmentXml, p12Certificate, p12Password);

        // 3. Request annulment to SRI
        boolean annulled = sriGateway.requestAnnulment(
                invoice.getAccessKey().getValue(),
                invoice.getAuthorizationNumber(),
                signedAnnulmentXml,
                invoice.getEnvironment()
        );

        if (!annulled) {
            throw new RuntimeException("El SRI rechazó la solicitud de anulación");
        }

        // 4. Mark as annulled locally and save
        invoice.markAsAnnulled();
        return invoiceRepository.save(invoice);
    }

    private String generateAnnulmentXml(Invoice invoice) {
        // Here we would use a serializer specific for the annulment XML schema
        // For demonstration, we return a mock XML
        return "<mensajeCancelacion><claveAcceso>" + invoice.getAccessKey().getValue() + "</claveAcceso></mensajeCancelacion>";
    }
}
