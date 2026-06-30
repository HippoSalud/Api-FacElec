package com.factura.demo.infrastructure.adapter.out.persistence;

import com.factura.demo.application.port.out.NotaCreditoRepositoryPort;
import com.factura.demo.domain.model.NotaCredito;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryNotaCreditoRepositoryAdapter implements NotaCreditoRepositoryPort {

    private final ConcurrentHashMap<String, NotaCredito> storage = new ConcurrentHashMap<>();

    @Override
    public NotaCredito save(NotaCredito notaCredito) {
        storage.put(notaCredito.getId(), notaCredito);
        return notaCredito;
    }

    @Override
    public Optional<NotaCredito> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }
}
