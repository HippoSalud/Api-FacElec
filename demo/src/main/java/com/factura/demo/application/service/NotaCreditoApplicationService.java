package com.factura.demo.application.service;

import com.factura.demo.application.port.in.ProcessNotaCreditoUseCase;
import com.factura.demo.application.port.out.LocalDocumentStoragePort;
import com.factura.demo.application.port.out.NotaCreditoRepositoryPort;
import com.factura.demo.application.port.out.SignaturePort;
import com.factura.demo.application.port.out.SriGatewayPort;
import com.factura.demo.domain.model.AccessKey;
import com.factura.demo.domain.model.NotaCredito;
import com.factura.demo.application.port.out.SriGatewayPort.SriAuthorizationResult;
import com.factura.demo.domain.service.NotaCreditoXmlSerializer;

public class NotaCreditoApplicationService implements ProcessNotaCreditoUseCase {

    private final NotaCreditoRepositoryPort notaCreditoRepository;
    private final SignaturePort signaturePort;
    private final SriGatewayPort sriGateway;
    private final LocalDocumentStoragePort localDocumentStoragePort;

    public NotaCreditoApplicationService(
            NotaCreditoRepositoryPort notaCreditoRepository,
            SignaturePort signaturePort,
            SriGatewayPort sriGateway,
            LocalDocumentStoragePort localDocumentStoragePort) {
        this.notaCreditoRepository = notaCreditoRepository;
        this.signaturePort = signaturePort;
        this.sriGateway = sriGateway;
        this.localDocumentStoragePort = localDocumentStoragePort;
    }

    @Override
    public NotaCredito process(NotaCredito notaCredito, byte[] p12Certificate, String p12Password) {
        // 1. Generate Access Key
        String numericCode = "12345678";
        AccessKey accessKey = AccessKey.generate(
                notaCredito.getEmissionDate(),
                "04",
                notaCredito.getEmitterRuc(),
                notaCredito.getEnvironment(),
                notaCredito.getEstablishmentCode(),
                notaCredito.getEmissionPointCode(),
                notaCredito.getSequential(),
                numericCode,
                "1"
        );
        notaCredito.associateAccessKey(accessKey);

        // 2. Serialize to XML
        String unsignedXml = NotaCreditoXmlSerializer.serialize(notaCredito);
        notaCredito.setXmlContent(unsignedXml);

        // 3. Sign XML
        String signedXml = signaturePort.sign(unsignedXml, p12Certificate, p12Password);
        notaCredito.markAsSigned(signedXml);

        // 4. Send to SRI (Recepcion)
        boolean received = sriGateway.send(signedXml, notaCredito.getEnvironment());
        if (received) {
            notaCredito.markAsReceived();
        } else {
            notaCredito.markAsRejected();
            return notaCreditoRepository.save(notaCredito);
        }

        // 5. Check Authorization
        SriAuthorizationResult authResult = sriGateway.checkAuthorization(accessKey.getValue(), notaCredito.getEnvironment());
        if (authResult.authorized()) {
            notaCredito.markAsAuthorized(authResult.authorizationNumber(), authResult.authorizationDate());
        } else {
            notaCredito.markAsRejected();
        }

        // Save locally
        notaCreditoRepository.save(notaCredito);

        if (notaCredito.getStatus() == com.factura.demo.domain.model.CommercialDocument.Status.AUTORIZADA || 
            notaCredito.getStatus() == com.factura.demo.domain.model.CommercialDocument.Status.RECIBIDA) {
            localDocumentStoragePort.saveXml(notaCredito.getId(), notaCredito.getSignedXmlContent(), "notacredito");
        }

        return notaCredito;
    }
}
