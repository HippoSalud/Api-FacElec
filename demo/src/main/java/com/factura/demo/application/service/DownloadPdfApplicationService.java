package com.factura.demo.application.service;

import com.factura.demo.application.port.in.DownloadPdfUseCase;
import com.factura.demo.application.port.out.InvoiceRepositoryPort;
import com.factura.demo.application.port.out.NotaVentaRepositoryPort;
import com.factura.demo.application.port.out.PdfGeneratorPort;
import com.factura.demo.application.port.out.ProformaRepositoryPort;
import com.factura.demo.domain.model.CommercialDocument;

import java.util.Objects;

public class DownloadPdfApplicationService implements DownloadPdfUseCase {

    private final InvoiceRepositoryPort invoiceRepository;
    private final NotaVentaRepositoryPort notaVentaRepository;
    private final ProformaRepositoryPort proformaRepository;
    private final com.factura.demo.application.port.out.NotaCreditoRepositoryPort notaCreditoRepository;
    private final PdfGeneratorPort pdfGeneratorPort;

    public DownloadPdfApplicationService(
            InvoiceRepositoryPort invoiceRepository,
            NotaVentaRepositoryPort notaVentaRepository,
            ProformaRepositoryPort proformaRepository,
            com.factura.demo.application.port.out.NotaCreditoRepositoryPort notaCreditoRepository,
            PdfGeneratorPort pdfGeneratorPort
    ) {
        this.invoiceRepository = Objects.requireNonNull(invoiceRepository);
        this.notaVentaRepository = Objects.requireNonNull(notaVentaRepository);
        this.proformaRepository = Objects.requireNonNull(proformaRepository);
        this.notaCreditoRepository = Objects.requireNonNull(notaCreditoRepository);
        this.pdfGeneratorPort = Objects.requireNonNull(pdfGeneratorPort);
    }

    @Override
    public byte[] downloadPdf(String documentId, DocumentType type) {
        CommercialDocument document = switch (type) {
            case INVOICE -> invoiceRepository.findById(documentId)
                    .orElseThrow(() -> new IllegalArgumentException("Factura no encontrada"));
            case NOTA_VENTA -> notaVentaRepository.findById(documentId)
                    .orElseThrow(() -> new IllegalArgumentException("Nota de Venta no encontrada"));
            case PROFORMA -> proformaRepository.findById(documentId)
                    .orElseThrow(() -> new IllegalArgumentException("Proforma no encontrada"));
            case NOTA_CREDITO -> notaCreditoRepository.findById(documentId)
                    .orElseThrow(() -> new IllegalArgumentException("Nota de Crédito no encontrada"));
        };

        return pdfGeneratorPort.generateRide(document);
    }
}
