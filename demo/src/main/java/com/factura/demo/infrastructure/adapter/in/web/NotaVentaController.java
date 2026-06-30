package com.factura.demo.infrastructure.adapter.in.web;

import com.factura.demo.application.port.in.CancelNotaVentaUseCase;
import com.factura.demo.application.port.in.DownloadPdfUseCase;
import com.factura.demo.application.port.in.ProcessNotaVentaUseCase;
import com.factura.demo.application.port.out.NotaVentaRepositoryPort;
import com.factura.demo.domain.model.Client;
import com.factura.demo.domain.model.InvoiceLine;
import com.factura.demo.domain.model.NotaVenta;
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
@RequestMapping("/api/v1/notas-venta")
public class NotaVentaController {

    private final ProcessNotaVentaUseCase processNotaVentaUseCase;
    private final CancelNotaVentaUseCase cancelNotaVentaUseCase;
    private final DownloadPdfUseCase downloadPdfUseCase;
    private final NotaVentaRepositoryPort notaVentaRepository;

    public NotaVentaController(
            ProcessNotaVentaUseCase processNotaVentaUseCase,
            CancelNotaVentaUseCase cancelNotaVentaUseCase,
            DownloadPdfUseCase downloadPdfUseCase,
            NotaVentaRepositoryPort notaVentaRepository
    ) {
        this.processNotaVentaUseCase = processNotaVentaUseCase;
        this.cancelNotaVentaUseCase = cancelNotaVentaUseCase;
        this.downloadPdfUseCase = downloadPdfUseCase;
        this.notaVentaRepository = notaVentaRepository;
    }

    @PostMapping("/process")
    public ResponseEntity<NotaVentaResponseDto> processNotaVenta(@RequestBody NotaVentaRequestDto request) {
        Client client = new Client(
                request.client().identificationType(),
                request.client().identification(),
                request.client().name(),
                request.client().address(),
                request.client().email()
        );

        List<InvoiceLine> lines = request.lines().stream().map(lineDto -> {
            // Para Notas de Venta (RIMPE Negocio Popular) el IVA SIEMPRE debe ser 0%
            // Ignoramos los impuestos enviados por el usuario y forzamos código IVA 0%
            BigDecimal discount = lineDto.discount() != null ? lineDto.discount() : BigDecimal.ZERO;
            BigDecimal taxableBase = lineDto.unitPrice().multiply(lineDto.quantity()).subtract(discount);
            
            Tax zeroIva = new Tax("2", "0", BigDecimal.ZERO, taxableBase);
            return new InvoiceLine(lineDto.mainCode(), lineDto.description(), lineDto.quantity(), lineDto.unitPrice(), discount, List.of(zeroIva));
        }).collect(Collectors.toList());

        NotaVenta notaVenta = new NotaVenta(
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
        NotaVenta processed = processNotaVentaUseCase.process(notaVenta, p12Bytes, request.p12Password());
        return ResponseEntity.ok(mapToResponse(processed));
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotaVentaResponseDto> getNotaVenta(@PathVariable String id) {
        return notaVentaRepository.findById(id)
                .map(this::mapToResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<NotaVentaResponseDto> cancelNotaVenta(
            @PathVariable String id,
            @RequestBody CancelRequestDto request
    ) {
        byte[] p12Bytes = decodeP12Base64(request.p12CertificateBase64());
        NotaVenta canceled = cancelNotaVentaUseCase.cancel(id, p12Bytes, request.p12Password());
        return ResponseEntity.ok(mapToResponse(canceled));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable String id) {
        byte[] pdfBytes = downloadPdfUseCase.downloadPdf(id, DownloadPdfUseCase.DocumentType.NOTA_VENTA);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"nota-venta-" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    private NotaVentaResponseDto mapToResponse(NotaVenta doc) {
        return new NotaVentaResponseDto(
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

    private byte[] decodeP12(NotaVentaRequestDto request) {
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
    public record NotaVentaRequestDto(
            String emitterRuc, String emitterCompanyName, String emitterEstablishmentAddress,
            String establishmentCode, String emissionPointCode, String sequential,
            String obligationToKeepBooks, String environment, 
            String contributorRegime, String specialContributorResolution, String retentionAgent,
            ClientDto client,
            List<InvoiceLineDto> lines, String paymentMethod,
            String p12CertificateBase64, String p12Path, String p12Password
    ) {}

    public record CancelRequestDto(String p12CertificateBase64, String p12Path, String p12Password) {}
    public record ClientDto(String identificationType, String identification, String name, String address, String email) {}
    public record InvoiceLineDto(String mainCode, String description, BigDecimal quantity, BigDecimal unitPrice, BigDecimal discount, List<TaxDto> taxes) {}
    public record TaxDto(String code, String percentageCode, BigDecimal rate, BigDecimal taxableBase) {}
    public record NotaVentaResponseDto(String id, String accessKey, String status, String authorizationNumber, String authorizationDate, BigDecimal totalWithoutTax, BigDecimal totalTaxValue, BigDecimal totalAmount, String xmlContent) {}
}
