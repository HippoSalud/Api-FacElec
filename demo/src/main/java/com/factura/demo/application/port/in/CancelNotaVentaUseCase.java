package com.factura.demo.application.port.in;

import com.factura.demo.domain.model.NotaVenta;

public interface CancelNotaVentaUseCase {
    NotaVenta cancel(String notaVentaId, byte[] p12Certificate, String p12Password);
}
