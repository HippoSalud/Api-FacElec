package com.factura.demo.application.service;

import com.factura.demo.application.port.in.CancelNotaVentaUseCase;
import com.factura.demo.application.port.out.NotaVentaRepositoryPort;
import com.factura.demo.application.port.out.SignaturePort;
import com.factura.demo.application.port.out.SriGatewayPort;
import com.factura.demo.domain.model.CommercialDocument;
import com.factura.demo.domain.model.NotaVenta;

import java.util.Objects;

public class CancelNotaVentaApplicationService implements CancelNotaVentaUseCase {

    private final NotaVentaRepositoryPort notaVentaRepository;
    private final SignaturePort signaturePort;
    private final SriGatewayPort sriGateway;

    public CancelNotaVentaApplicationService(
            NotaVentaRepositoryPort notaVentaRepository,
            SignaturePort signaturePort,
            SriGatewayPort sriGateway
    ) {
        this.notaVentaRepository = Objects.requireNonNull(notaVentaRepository);
        this.signaturePort = Objects.requireNonNull(signaturePort);
        this.sriGateway = Objects.requireNonNull(sriGateway);
    }

    @Override
    public NotaVenta cancel(String notaVentaId, byte[] p12Certificate, String p12Password) {
        NotaVenta notaVenta = notaVentaRepository.findById(notaVentaId)
                .orElseThrow(() -> new IllegalArgumentException("Nota de Venta no encontrada"));

        if (notaVenta.getStatus() != CommercialDocument.Status.AUTORIZADA) {
            throw new IllegalStateException("Solo se pueden anular notas de venta en estado AUTORIZADA en el SRI");
        }

        String annulmentXml = "<mensajeCancelacion><claveAcceso>" + notaVenta.getAccessKey().getValue() + "</claveAcceso></mensajeCancelacion>";
        String signedAnnulmentXml = signaturePort.sign(annulmentXml, p12Certificate, p12Password);

        boolean annulled = sriGateway.requestAnnulment(
                notaVenta.getAccessKey().getValue(),
                notaVenta.getAuthorizationNumber(),
                signedAnnulmentXml,
                notaVenta.getEnvironment()
        );

        if (!annulled) {
            throw new RuntimeException("El SRI rechazó la solicitud de anulación");
        }

        notaVenta.markAsAnnulled();
        return notaVentaRepository.save(notaVenta);
    }
}
