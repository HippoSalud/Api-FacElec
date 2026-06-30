package com.factura.demo.infrastructure.adapter.out.persistence;

import com.factura.demo.application.port.out.ProformaRepositoryPort;
import com.factura.demo.domain.model.Proforma;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryProformaRepositoryAdapter implements ProformaRepositoryPort {

    private final Map<String, Proforma> store = new ConcurrentHashMap<>();

    @Override
    public Proforma save(Proforma proforma) {
        store.put(proforma.getId(), proforma);
        return proforma;
    }

    @Override
    public Optional<Proforma> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }
}
