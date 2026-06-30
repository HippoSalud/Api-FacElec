package com.factura.demo.application.service;

import com.factura.demo.application.port.in.ProcessProformaUseCase;
import com.factura.demo.application.port.out.ProformaRepositoryPort;
import com.factura.demo.domain.model.Proforma;

import java.util.Objects;

public class ProformaApplicationService implements ProcessProformaUseCase {

    private final ProformaRepositoryPort proformaRepository;

    public ProformaApplicationService(ProformaRepositoryPort proformaRepository) {
        this.proformaRepository = Objects.requireNonNull(proformaRepository);
    }

    @Override
    public Proforma process(Proforma proforma) {
        // Proformas are internal documents. They don't need SRI signing or authorization.
        // We just save them as created.
        return proformaRepository.save(proforma);
    }
}
