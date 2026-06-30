package com.factura.demo.infrastructure.adapter.in.web;

import com.factura.demo.application.port.in.DownloadPdfUseCase;
import com.factura.demo.application.port.in.ProcessProformaUseCase;
import com.factura.demo.application.port.out.ProformaRepositoryPort;
import com.factura.demo.domain.model.Client;
import com.factura.demo.domain.model.InvoiceLine;
import com.factura.demo.domain.model.Proforma;
import com.factura.demo.domain.model.Tax;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/proformas")
public class ProformaController {

    private final ProcessProformaUseCase processProformaUseCase;
    private final DownloadPdfUseCase downloadPdfUseCase;
    private final ProformaRepositoryPort proformaRepository;

    public ProformaController(
            ProcessProformaUseCase processProformaUseCase,
            DownloadPdfUseCase downloadPdfUseCase,
            ProformaRepositoryPort proformaRepository
    ) {
        this.processProformaUseCase = processProformaUseCase;
        this.downloadPdfUseCase = downloadPdfUseCase;
        this.proformaRepository = proformaRepository;
    }

    @PostMapping("/process")
    public ResponseEntity<ProformaResponseDto> processProforma(@RequestBody ProformaRequestDto request) {
        Client client = new Client(
                request.client().identificationType(), request.client().identification(),
                request.client().name(), request.client().address(), request.client().email()
        );

        List<InvoiceLine> lines = request.lines().stream().map(lineDto -> {
            List<Tax> taxes = lineDto.taxes().stream().map(taxDto -> new Tax(
                    taxDto.code(), taxDto.percentageCode(), taxDto.rate(), taxDto.taxableBase()
            )).collect(Collectors.toList());

            return new InvoiceLine(lineDto.mainCode(), lineDto.description(), lineDto.quantity(), lineDto.unitPrice(), lineDto.discount(), taxes);
        }).collect(Collectors.toList());

        Proforma proforma = new Proforma(
                UUID.randomUUID().toString(), request.emitterRuc(), request.emitterCompanyName(),
                request.emitterEstablishmentAddress(), request.establishmentCode(), request.emissionPointCode(),
                request.sequential(), LocalDate.now(), client, lines, request.paymentMethod()
        );

        Proforma processed = processProformaUseCase.process(proforma);
        return ResponseEntity.ok(mapToResponse(processed));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProformaResponseDto> getProforma(@PathVariable String id) {
        return proformaRepository.findById(id)
                .map(this::mapToResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable String id) {
        byte[] pdfBytes = downloadPdfUseCase.downloadPdf(id, DownloadPdfUseCase.DocumentType.PROFORMA);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"proforma-" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    private ProformaResponseDto mapToResponse(Proforma doc) {
        return new ProformaResponseDto(
                doc.getId(), doc.getStatus().name(), doc.getTotalWithoutTax(),
                doc.getTotalTaxValue(), doc.getTotalAmount()
        );
    }

    // DTO Definitions
    public record ProformaRequestDto(
            String emitterRuc, String emitterCompanyName, String emitterEstablishmentAddress,
            String establishmentCode, String emissionPointCode, String sequential,
            ClientDto client, List<InvoiceLineDto> lines, String paymentMethod
    ) {}

    public record ClientDto(String identificationType, String identification, String name, String address, String email) {}
    public record InvoiceLineDto(String mainCode, String description, BigDecimal quantity, BigDecimal unitPrice, BigDecimal discount, List<TaxDto> taxes) {}
    public record TaxDto(String code, String percentageCode, BigDecimal rate, BigDecimal taxableBase) {}
    public record ProformaResponseDto(String id, String status, BigDecimal totalWithoutTax, BigDecimal totalTaxValue, BigDecimal totalAmount) {}
}
