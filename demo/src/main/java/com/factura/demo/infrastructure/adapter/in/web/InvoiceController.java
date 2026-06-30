package com.factura.demo.infrastructure.adapter.in.web;

import com.factura.demo.application.port.in.CancelInvoiceUseCase;
import com.factura.demo.application.port.in.DownloadPdfUseCase;
import com.factura.demo.application.port.in.ProcessInvoiceUseCase;
import com.factura.demo.application.port.out.InvoiceRepositoryPort;
import com.factura.demo.domain.model.Client;
import com.factura.demo.domain.model.Invoice;
import com.factura.demo.domain.model.InvoiceLine;
import com.factura.demo.domain.model.Tax;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/invoices")
public class InvoiceController {

    private final ProcessInvoiceUseCase processInvoiceUseCase;
    private final CancelInvoiceUseCase cancelInvoiceUseCase;
    private final DownloadPdfUseCase downloadPdfUseCase;
    private final InvoiceRepositoryPort invoiceRepository;

    public InvoiceController(
            ProcessInvoiceUseCase processInvoiceUseCase,
            CancelInvoiceUseCase cancelInvoiceUseCase,
            DownloadPdfUseCase downloadPdfUseCase,
            InvoiceRepositoryPort invoiceRepository
    ) {
        this.processInvoiceUseCase = processInvoiceUseCase;
        this.cancelInvoiceUseCase = cancelInvoiceUseCase;
        this.downloadPdfUseCase = downloadPdfUseCase;
        this.invoiceRepository = invoiceRepository;
    }

    @PostMapping("/process")
    public ResponseEntity<InvoiceResponseDto> processInvoice(@RequestBody InvoiceRequestDto request) {
        // Map DTOs to Domain Entities
        Client client = new Client(
                request.client().identificationType(),
                request.client().identification(),
                request.client().name(),
                request.client().address(),
                request.client().email()
        );

        List<InvoiceLine> lines = request.lines().stream().map(lineDto -> {
            List<Tax> taxes = lineDto.taxes().stream().map(taxDto -> new Tax(
                    taxDto.code(),
                    taxDto.percentageCode(),
                    taxDto.rate(),
                    taxDto.taxableBase()
            )).collect(Collectors.toList());

            return new InvoiceLine(
                    lineDto.mainCode(),
                    lineDto.description(),
                    lineDto.quantity(),
                    lineDto.unitPrice(),
                    lineDto.discount(),
                    taxes
            );
        }).collect(Collectors.toList());

        Invoice invoice = new Invoice(
                UUID.randomUUID().toString(),
                request.emitterRuc(),
                request.emitterCompanyName(),
                request.emitterEstablishmentAddress(),
                request.establishmentCode(),
                request.emissionPointCode(),
                request.sequential(),
                request.obligationToKeepBooks(),
                request.environment(),
                request.contributorRegime(),
                request.specialContributorResolution(),
                request.retentionAgent(),
                LocalDate.now(),
                client,
                lines,
                request.paymentMethod()
        );

        byte[] p12Bytes = decodeP12(request);

        Invoice processedInvoice = processInvoiceUseCase.process(invoice, p12Bytes, request.p12Password());
        return ResponseEntity.ok(mapToResponse(processedInvoice));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvoiceResponseDto> getInvoice(@PathVariable String id) {
        return invoiceRepository.findById(id)
                .map(this::mapToResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<InvoiceResponseDto> cancelInvoice(
            @PathVariable String id,
            @RequestBody CancelRequestDto request
    ) {
        byte[] p12Bytes = decodeP12(request.p12CertificateBase64(), request.p12Path());
        Invoice canceledInvoice = cancelInvoiceUseCase.cancel(id, p12Bytes, request.p12Password());
        return ResponseEntity.ok(mapToResponse(canceledInvoice));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable String id) {
        byte[] pdfBytes = downloadPdfUseCase.downloadPdf(id, DownloadPdfUseCase.DocumentType.INVOICE);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"factura-" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    private InvoiceResponseDto mapToResponse(Invoice invoice) {
        return new InvoiceResponseDto(
                invoice.getId(),
                invoice.getAccessKey() != null ? invoice.getAccessKey().getValue() : null,
                invoice.getStatus().name(),
                invoice.getAuthorizationNumber(),
                invoice.getAuthorizationDate() != null ? invoice.getAuthorizationDate().toString() : null,
                invoice.getTotalWithoutTax(),
                invoice.getTotalTaxValue(),
                invoice.getTotalAmount(),
                invoice.getSignedXmlContent()
        );
    }

    private byte[] decodeP12(InvoiceRequestDto request) {
        return decodeP12(request.p12CertificateBase64(), request.p12Path());
    }

    private byte[] decodeP12(String base64, String path) {
        if (path != null && !path.isBlank()) {
            try {
                return java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(path));
            } catch (java.io.IOException e) {
                throw new RuntimeException("Error al leer el archivo de firma: " + e.getMessage(), e);
            }
        } else if (base64 != null && !base64.isBlank()) {
            return decodeP12Base64(base64);
        }
        return null;
    }

    private byte[] decodeP12Base64(String base64) {
        if (base64 == null || base64.isBlank()) return null;
        try {
            return Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            String cleanB64 = base64.replaceAll("[^A-Za-z0-9+/_-]", "").replace('-', '+').replace('_', '/');
            int mod = cleanB64.length() % 4;
            if (mod == 2) cleanB64 += "==";
            else if (mod == 3) cleanB64 += "=";
            return Base64.getDecoder().decode(cleanB64);
        }
    }

    // DTO Definitions
    public record InvoiceRequestDto(
            String emitterRuc,
            String emitterCompanyName,
            String emitterEstablishmentAddress,
            String establishmentCode,
            String emissionPointCode,
            String sequential,
            String obligationToKeepBooks,
            String environment,
            String contributorRegime,
            String specialContributorResolution,
            String retentionAgent,
            ClientDto client,
            List<InvoiceLineDto> lines,
            String paymentMethod,
            String p12CertificateBase64,
            String p12Path,
            String p12Password
    ) {}

    public record CancelRequestDto(
            String p12CertificateBase64,
            String p12Path,
            String p12Password
    ) {}

    public record ClientDto(
            String identificationType,
            String identification,
            String name,
            String address,
            String email
    ) {}

    public record InvoiceLineDto(
            String mainCode,
            String description,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal discount,
            List<TaxDto> taxes
    ) {}

    public record TaxDto(
            String code,
            String percentageCode,
            BigDecimal rate,
            BigDecimal taxableBase
    ) {}

    public record InvoiceResponseDto(
            String id,
            String accessKey,
            String status,
            String authorizationNumber,
            String authorizationDate,
            BigDecimal totalWithoutTax,
            BigDecimal totalTaxValue,
            BigDecimal totalAmount,
            String xmlContent
    ) {}
}
