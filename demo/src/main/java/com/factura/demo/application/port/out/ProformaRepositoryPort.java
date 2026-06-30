package com.factura.demo.application.port.out;

import com.factura.demo.domain.model.Proforma;
import java.util.Optional;

public interface ProformaRepositoryPort {
    Proforma save(Proforma proforma);
    Optional<Proforma> findById(String id);
}
