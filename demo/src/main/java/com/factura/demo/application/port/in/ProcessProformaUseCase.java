package com.factura.demo.application.port.in;

import com.factura.demo.domain.model.Proforma;

public interface ProcessProformaUseCase {
    Proforma process(Proforma proforma);
}
