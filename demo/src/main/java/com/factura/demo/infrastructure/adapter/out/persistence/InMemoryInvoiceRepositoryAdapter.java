package com.factura.demo.infrastructure.adapter.out.persistence;

import com.factura.demo.application.port.out.InvoiceRepositoryPort;
import com.factura.demo.domain.model.Invoice;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryInvoiceRepositoryAdapter implements InvoiceRepositoryPort {

    private final Map<String, Invoice> database = new ConcurrentHashMap<>();

    @Override
    public Invoice save(Invoice invoice) {
        database.put(invoice.getId(), invoice);
        return invoice;
    }

    @Override
    public Optional<Invoice> findById(String id) {
        return Optional.ofNullable(database.get(id));
    }
}
