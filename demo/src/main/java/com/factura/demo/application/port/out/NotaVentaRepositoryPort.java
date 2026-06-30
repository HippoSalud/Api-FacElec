package com.factura.demo.application.port.out;

import com.factura.demo.domain.model.NotaVenta;
import java.util.Optional;

public interface NotaVentaRepositoryPort {
    NotaVenta save(NotaVenta notaVenta);
    Optional<NotaVenta> findById(String id);
}
