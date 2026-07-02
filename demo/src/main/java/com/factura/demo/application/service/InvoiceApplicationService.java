package com.factura.demo.application.service;

import com.factura.demo.application.port.in.ProcessInvoiceUseCase;
import com.factura.demo.application.port.out.InvoiceRepositoryPort;
import com.factura.demo.application.port.out.SignaturePort;
import com.factura.demo.application.port.out.SriGatewayPort;
import com.factura.demo.application.port.out.SriGatewayPort.SriAuthorizationResult;
import com.factura.demo.domain.model.AccessKey;
import com.factura.demo.domain.model.Invoice;
import com.factura.demo.domain.service.InvoiceXmlSerializer;

import java.util.Objects;

public class InvoiceApplicationService implements ProcessInvoiceUseCase {

    private final InvoiceRepositoryPort invoiceRepository;
    private final SignaturePort signaturePort;
    private final SriGatewayPort sriGateway;
    private final com.factura.demo.application.port.out.DocumentStoragePort localDocumentStorage;

    public InvoiceApplicationService(
            InvoiceRepositoryPort invoiceRepository,
            SignaturePort signaturePort,
            SriGatewayPort sriGateway,
            com.factura.demo.application.port.out.DocumentStoragePort localDocumentStorage
    ) {
        this.invoiceRepository = Objects.requireNonNull(invoiceRepository);
        this.signaturePort = Objects.requireNonNull(signaturePort);
        this.sriGateway = Objects.requireNonNull(sriGateway);
        this.localDocumentStorage = Objects.requireNonNull(localDocumentStorage);
    }

    @Override
    public Invoice process(Invoice invoice, byte[] p12Certificate, String p12Password) {
        // 1. Generate and associate Access Key
        String numericCode = "12345678"; // Can be randomized or generated
        AccessKey accessKey = AccessKey.generate(
                invoice.getEmissionDate(),
                "01", // "01" represents Factura
                invoice.getEmitterRuc(),
                invoice.getEnvironment(),
                invoice.getEstablishmentCode(),
                invoice.getEmissionPointCode(),
                invoice.getSequential(),
                numericCode,
                "1" // "1" represents Normal emission type
        );
        invoice.associateAccessKey(accessKey);

        // 2. Generate XML Content
        String unsignedXml = InvoiceXmlSerializer.serialize(invoice);
        invoice.setXmlContent(unsignedXml);

        // 3. Digitally sign the XML using XAdES-BES
        String signedXml = signaturePort.sign(unsignedXml, p12Certificate, p12Password);
        invoice.markAsSigned(signedXml);

        // Save progress in repository
        invoiceRepository.save(invoice);

        // 4. Send to SRI Recepcion
        boolean received = sriGateway.send(signedXml, invoice.getEnvironment());
        if (!received) {
            invoice.markAsRejected();
            return invoiceRepository.save(invoice);
        }
        invoice.markAsReceived();
        invoiceRepository.save(invoice);

        // 5. Query SRI Autorizacion
        SriAuthorizationResult authResult = sriGateway.checkAuthorization(accessKey.getValue(), invoice.getEnvironment());
        if (authResult.authorized()) {
            invoice.markAsAuthorized(authResult.authorizationNumber(), authResult.authorizationDate());
        } else {
            invoice.markAsRejected();
        }

        invoiceRepository.save(invoice);

        if (invoice.getStatus() == com.factura.demo.domain.model.CommercialDocument.Status.AUTORIZADA || 
            invoice.getStatus() == com.factura.demo.domain.model.CommercialDocument.Status.RECIBIDA) {
            localDocumentStorage.saveXml(invoice.getId(), invoice.getSignedXmlContent(), "factura");
        }

        return invoice;
    }
}
