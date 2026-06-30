package com.factura.demo.application.service;

import com.factura.demo.application.port.in.ProcessNotaVentaUseCase;
import com.factura.demo.application.port.out.NotaVentaRepositoryPort;
import com.factura.demo.application.port.out.SignaturePort;
import com.factura.demo.application.port.out.SriGatewayPort;
import com.factura.demo.application.port.out.SriGatewayPort.SriAuthorizationResult;
import com.factura.demo.domain.model.AccessKey;
import com.factura.demo.domain.model.NotaVenta;
import com.factura.demo.domain.service.InvoiceXmlSerializer; // We will reuse or create a specific one

import java.util.Objects;

public class NotaVentaApplicationService implements ProcessNotaVentaUseCase {

    private final NotaVentaRepositoryPort notaVentaRepository;
    private final SignaturePort signaturePort;
    private final SriGatewayPort sriGateway;
    private final com.factura.demo.application.port.out.LocalDocumentStoragePort localDocumentStorage;

    public NotaVentaApplicationService(
            NotaVentaRepositoryPort notaVentaRepository,
            SignaturePort signaturePort,
            SriGatewayPort sriGateway,
            com.factura.demo.application.port.out.LocalDocumentStoragePort localDocumentStorage
    ) {
        this.notaVentaRepository = Objects.requireNonNull(notaVentaRepository);
        this.signaturePort = Objects.requireNonNull(signaturePort);
        this.sriGateway = Objects.requireNonNull(sriGateway);
        this.localDocumentStorage = Objects.requireNonNull(localDocumentStorage);
    }

    @Override
    public NotaVenta process(NotaVenta notaVenta, byte[] p12Certificate, String p12Password) {
        // 1. Generate and associate Access Key (Nota de Venta electrónica en SRI se envía como Factura '01')
        String numericCode = "12345678";
        AccessKey accessKey = AccessKey.generate(
                notaVenta.getEmissionDate(),
                "01", // SRI NO tiene esquema XML para 'Nota Venta', se usa '01' (Factura)
                notaVenta.getEmitterRuc(),
                notaVenta.getEnvironment(),
                notaVenta.getEstablishmentCode(),
                notaVenta.getEmissionPointCode(),
                notaVenta.getSequential(),
                numericCode,
                "1" 
        );
        notaVenta.associateAccessKey(accessKey);

        // 2. Generate XML Content (Usando formato de Factura para el SRI)
        com.factura.demo.domain.model.Invoice tempInvoice = new com.factura.demo.domain.model.Invoice(
                notaVenta.getId(), notaVenta.getEmitterRuc(), notaVenta.getEmitterCompanyName(),
                notaVenta.getEmitterEstablishmentAddress(), notaVenta.getEstablishmentCode(),
                notaVenta.getEmissionPointCode(), notaVenta.getSequential(), notaVenta.getObligationToKeepBooks(),
                notaVenta.getEnvironment(), notaVenta.getContributorRegime(), notaVenta.getSpecialContributorResolution(),
                notaVenta.getRetentionAgent(), notaVenta.getEmissionDate(), notaVenta.getClient(),
                notaVenta.getLines(), notaVenta.getPaymentMethod()
        );
        tempInvoice.associateAccessKey(accessKey);
        
        String unsignedXml = com.factura.demo.domain.service.InvoiceXmlSerializer.serialize(tempInvoice);
        notaVenta.setXmlContent(unsignedXml);

        // 3. Digitally sign the XML using XAdES-BES
        String signedXml = signaturePort.sign(unsignedXml, p12Certificate, p12Password);
        notaVenta.markAsSigned(signedXml);

        // Save progress in repository
        notaVentaRepository.save(notaVenta);

        // 4. Send to SRI Recepcion
        boolean received = sriGateway.send(signedXml, notaVenta.getEnvironment());
        if (!received) {
            notaVenta.markAsRejected();
            return notaVentaRepository.save(notaVenta);
        }
        notaVenta.markAsReceived();
        notaVentaRepository.save(notaVenta);

        // 5. Query SRI Autorizacion
        SriAuthorizationResult authResult = sriGateway.checkAuthorization(accessKey.getValue(), notaVenta.getEnvironment());
        if (authResult.authorized()) {
            notaVenta.markAsAuthorized(authResult.authorizationNumber(), authResult.authorizationDate());
        } else {
            notaVenta.markAsRejected();
        }

        notaVentaRepository.save(notaVenta);

        if (notaVenta.getStatus() == com.factura.demo.domain.model.CommercialDocument.Status.AUTORIZADA || 
            notaVenta.getStatus() == com.factura.demo.domain.model.CommercialDocument.Status.RECIBIDA) {
            localDocumentStorage.saveXml(notaVenta.getId(), notaVenta.getSignedXmlContent(), "notaventa");
        }

        return notaVenta;
    }
}
