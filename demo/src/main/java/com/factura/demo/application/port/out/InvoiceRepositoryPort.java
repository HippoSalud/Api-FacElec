package com.factura.demo.application.port.out;

import com.factura.demo.domain.model.Invoice;
import java.util.Optional;

public interface InvoiceRepositoryPort {
    Invoice save(Invoice invoice);
    Optional<Invoice> findById(String id);
}
