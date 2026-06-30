package com.factura.demo.application.port.in;

import com.factura.demo.domain.model.NotaCredito;

public interface ProcessNotaCreditoUseCase {
    NotaCredito process(NotaCredito notaCredito, byte[] p12Certificate, String p12Password);
}
