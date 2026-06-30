package com.factura.demo.application.port.in;

import com.factura.demo.domain.model.NotaVenta;

public interface ProcessNotaVentaUseCase {
    NotaVenta process(NotaVenta notaVenta, byte[] p12Certificate, String p12Password);
}
