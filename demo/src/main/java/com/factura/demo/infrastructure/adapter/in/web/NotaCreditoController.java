package com.factura.demo.infrastructure.adapter.in.web;

import com.factura.demo.application.port.in.ProcessNotaCreditoUseCase;
import com.factura.demo.application.port.out.NotaCreditoRepositoryPort;
import com.factura.demo.domain.model.Client;
import com.factura.demo.domain.model.InvoiceLine;
import com.factura.demo.domain.model.NotaCredito;
import com.factura.demo.domain.model.Tax;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/notas-credito")
public class NotaCreditoController {

    private final ProcessNotaCreditoUseCase processNotaCreditoUseCase;
    private final NotaCreditoRepositoryPort notaCreditoRepository;
    private final com.factura.demo.application.port.in.DownloadPdfUseCase downloadPdfUseCase;

    public NotaCreditoController(
            ProcessNotaCreditoUseCase processNotaCreditoUseCase,
            NotaCreditoRepositoryPort notaCreditoRepository,
            com.factura.demo.application.port.in.DownloadPdfUseCase downloadPdfUseCase
    ) {
        this.processNotaCreditoUseCase = processNotaCreditoUseCase;
        this.notaCreditoRepository = notaCreditoRepository;
        this.downloadPdfUseCase = downloadPdfUseCase;
    }

    @PostMapping("/process")
    public ResponseEntity<NotaCreditoResponseDto> processNotaCredito(@RequestBody NotaCreditoRequestDto request) {
        Client client = new Client(
                request.client().identificationType(),
                request.client().identification(),
                request.client().name(),
                request.client().address(),
                request.client().email()
        );

        List<InvoiceLine> lines = request.lines().stream().map(lineDto -> {
            List<Tax> taxes = lineDto.taxes().stream().map(taxDto -> new Tax(
                    taxDto.code(), taxDto.percentageCode(), taxDto.rate(), taxDto.taxableBase()
            )).collect(Collectors.toList());

            return new InvoiceLine(lineDto.mainCode(), lineDto.description(), lineDto.quantity(), lineDto.unitPrice(), lineDto.discount(), taxes);
        }).collect(Collectors.toList());

        NotaCredito notaCredito = new NotaCredito(
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
                java.time.LocalDate.now(java.time.ZoneId.of("America/Guayaquil")),
                client,
                lines,
                request.paymentMethod(),
                request.modifiedDocumentType(),
                request.modifiedDocumentId(),
                LocalDate.parse(request.modifiedDocumentEmissionDate()), // expects YYYY-MM-DD
                request.modificationReason()
        );

        byte[] p12Bytes = decodeP12(request);
        NotaCredito processed = processNotaCreditoUseCase.process(notaCredito, p12Bytes, request.p12Password());
        return ResponseEntity.ok(mapToResponse(processed));
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotaCreditoResponseDto> getNotaCredito(@PathVariable String id) {
        return notaCreditoRepository.findById(id)
                .map(this::mapToResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable String id) {
        byte[] pdfBytes = downloadPdfUseCase.downloadPdf(id, com.factura.demo.application.port.in.DownloadPdfUseCase.DocumentType.NOTA_CREDITO);
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"nota-credito-" + id + ".pdf\"")
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    private NotaCreditoResponseDto mapToResponse(NotaCredito doc) {
        return new NotaCreditoResponseDto(
                doc.getId(),
                doc.getAccessKey() != null ? doc.getAccessKey().getValue() : null,
                doc.getStatus().name(),
                doc.getAuthorizationNumber(),
                doc.getAuthorizationDate() != null ? doc.getAuthorizationDate().toString() : null,
                doc.getTotalWithoutTax(),
                doc.getTotalTaxValue(),
                doc.getTotalAmount(),
                doc.getSignedXmlContent()
        );
    }

    private byte[] decodeP12(NotaCreditoRequestDto request) {
        if (request.p12Path() != null && !request.p12Path().isBlank()) {
            try {
                return java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(request.p12Path()));
            } catch (java.io.IOException e) {
                throw new RuntimeException("Error al leer el archivo de firma: " + e.getMessage(), e);
            }
        } else if (request.p12CertificateBase64() != null && !request.p12CertificateBase64().isBlank()) {
            return decodeP12Base64(request.p12CertificateBase64());
        }
        return null;
    }

    private byte[] decodeP12Base64(String base64) {
        if (base64 == null || base64.isBlank()) return null;
        try { return Base64.getDecoder().decode(base64); } 
        catch (IllegalArgumentException e) {
            String cleanB64 = base64.replaceAll("[^A-Za-z0-9+/_-]", "").replace('-', '+').replace('_', '/');
            int mod = cleanB64.length() % 4;
            if (mod == 2) cleanB64 += "=="; else if (mod == 3) cleanB64 += "=";
            return Base64.getDecoder().decode(cleanB64);
        }
    }

    // DTO Definitions
    public record NotaCreditoRequestDto(
            String emitterRuc, String emitterCompanyName, String emitterEstablishmentAddress,
            String establishmentCode, String emissionPointCode, String sequential,
            String obligationToKeepBooks, String environment, 
            String contributorRegime, String specialContributorResolution, String retentionAgent,
            ClientDto client,
            List<InvoiceLineDto> lines, String paymentMethod,
            String modifiedDocumentType, String modifiedDocumentId, String modifiedDocumentEmissionDate, String modificationReason,
            String p12CertificateBase64, String p12Path, String p12Password
    ) {}

    public record ClientDto(String identificationType, String identification, String name, String address, String email) {}
    public record InvoiceLineDto(String mainCode, String description, BigDecimal quantity, BigDecimal unitPrice, BigDecimal discount, List<TaxDto> taxes) {}
    public record TaxDto(String code, String percentageCode, BigDecimal rate, BigDecimal taxableBase) {}
    public record NotaCreditoResponseDto(String id, String accessKey, String status, String authorizationNumber, String authorizationDate, BigDecimal totalWithoutTax, BigDecimal totalTaxValue, BigDecimal totalAmount, String xmlContent) {}
}
