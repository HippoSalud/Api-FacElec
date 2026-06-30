package com.factura.demo.application.port.out;

import com.factura.demo.domain.model.NotaCredito;

import java.util.Optional;

public interface NotaCreditoRepositoryPort {
    NotaCredito save(NotaCredito notaCredito);
    Optional<NotaCredito> findById(String id);
}
