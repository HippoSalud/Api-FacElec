package com.factura.demo.infrastructure.adapter.out.persistence;

import com.factura.demo.application.port.out.NotaVentaRepositoryPort;
import com.factura.demo.domain.model.NotaVenta;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryNotaVentaRepositoryAdapter implements NotaVentaRepositoryPort {

    private final Map<String, NotaVenta> store = new ConcurrentHashMap<>();

    @Override
    public NotaVenta save(NotaVenta notaVenta) {
        store.put(notaVenta.getId(), notaVenta);
        return notaVenta;
    }

    @Override
    public Optional<NotaVenta> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }
}
