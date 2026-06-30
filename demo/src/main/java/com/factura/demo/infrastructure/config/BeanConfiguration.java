package com.factura.demo.infrastructure.config;

import com.factura.demo.application.port.in.*;
import com.factura.demo.application.port.out.*;
import com.factura.demo.application.service.*;

import com.factura.demo.infrastructure.adapter.out.persistence.InMemoryInvoiceRepositoryAdapter;
import com.factura.demo.infrastructure.adapter.out.persistence.InMemoryNotaVentaRepositoryAdapter;
import com.factura.demo.infrastructure.adapter.out.persistence.InMemoryProformaRepositoryAdapter;
import com.factura.demo.infrastructure.adapter.out.signature.SignatureAdapter;
import com.factura.demo.infrastructure.adapter.out.sri.SriSoapAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfiguration {

    @Bean
    public InvoiceRepositoryPort invoiceRepositoryPort() {
        return new InMemoryInvoiceRepositoryAdapter();
    }

    @Bean
    public NotaVentaRepositoryPort notaVentaRepositoryPort() {
        return new InMemoryNotaVentaRepositoryAdapter();
    }

    @Bean
    public ProformaRepositoryPort proformaRepositoryPort() {
        return new InMemoryProformaRepositoryAdapter();
    }

    @Bean
    public SignaturePort signaturePort(SriProperties sriProperties) {
        return new SignatureAdapter(sriProperties);
    }

    @Bean
    public SriGatewayPort sriGatewayPort(SriProperties sriProperties) {
        return new SriSoapAdapter(sriProperties);
    }


    @Bean
    public com.factura.demo.application.port.out.LocalDocumentStoragePort localDocumentStoragePort() {
        return new com.factura.demo.infrastructure.adapter.out.storage.LocalFileSystemStorageAdapter("./comprobantes_generados");
    }

    @Bean
    public ProcessInvoiceUseCase processInvoiceUseCase(
            InvoiceRepositoryPort invoiceRepositoryPort,
            SignaturePort signaturePort,
            SriGatewayPort sriGatewayPort,
            com.factura.demo.application.port.out.LocalDocumentStoragePort localDocumentStoragePort) {
        return new InvoiceApplicationService(invoiceRepositoryPort, signaturePort, sriGatewayPort, localDocumentStoragePort);
    }

    @Bean
    public CancelInvoiceUseCase cancelInvoiceUseCase(
            InvoiceRepositoryPort invoiceRepositoryPort,
            SignaturePort signaturePort,
            SriGatewayPort sriGatewayPort
    ) {
        return new CancelInvoiceApplicationService(invoiceRepositoryPort, signaturePort, sriGatewayPort);
    }

    @Bean
    public ProcessNotaVentaUseCase processNotaVentaUseCase(
            NotaVentaRepositoryPort notaVentaRepositoryPort,
            SignaturePort signaturePort,
            SriGatewayPort sriGatewayPort,
            com.factura.demo.application.port.out.LocalDocumentStoragePort localDocumentStoragePort
    ) {
        return new NotaVentaApplicationService(notaVentaRepositoryPort, signaturePort, sriGatewayPort, localDocumentStoragePort);
    }

    @Bean
    public CancelNotaVentaUseCase cancelNotaVentaUseCase(
            NotaVentaRepositoryPort notaVentaRepository,
            SignaturePort signaturePort,
            SriGatewayPort sriGateway
    ) {
        return new CancelNotaVentaApplicationService(notaVentaRepository, signaturePort, sriGateway);
    }

    @Bean
    public ProcessProformaUseCase processProformaUseCase(
            ProformaRepositoryPort proformaRepository
    ) {
        return new ProformaApplicationService(proformaRepository);
    }

    @Bean
    public DownloadPdfUseCase downloadPdfUseCase(
            InvoiceRepositoryPort invoiceRepository,
            NotaVentaRepositoryPort notaVentaRepository,
            ProformaRepositoryPort proformaRepository,
            com.factura.demo.application.port.out.NotaCreditoRepositoryPort notaCreditoRepository,
            PdfGeneratorPort pdfGeneratorPort
    ) {
        return new DownloadPdfApplicationService(invoiceRepository, notaVentaRepository, proformaRepository, notaCreditoRepository, pdfGeneratorPort);
    }

    @Bean
    public com.factura.demo.application.port.in.ProcessNotaCreditoUseCase processNotaCreditoUseCase(
            com.factura.demo.application.port.out.NotaCreditoRepositoryPort notaCreditoRepositoryPort,
            SignaturePort signaturePort,
            SriGatewayPort sriGatewayPort,
            com.factura.demo.application.port.out.LocalDocumentStoragePort localDocumentStoragePort) {
        return new com.factura.demo.application.service.NotaCreditoApplicationService(
                notaCreditoRepositoryPort, signaturePort, sriGatewayPort, localDocumentStoragePort);
    }
}
