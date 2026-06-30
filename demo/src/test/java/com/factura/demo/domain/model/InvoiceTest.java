package com.factura.demo.domain.model;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class InvoiceTest {

    @Test
    void shouldCalculateCorrectInvoiceTotals() {
        Client client = new Client("05", "1724567890", "Juan Pérez", "Quito, Ecuador", "juan@test.com");
        
        Tax tax = new Tax("2", "4", BigDecimal.valueOf(15.0), BigDecimal.valueOf(10.00)); // 15% IVA on $10.00 = $1.50
        InvoiceLine line1 = new InvoiceLine("P001", "Desarrollo Software", BigDecimal.ONE, BigDecimal.valueOf(10.00), BigDecimal.ZERO, List.of(tax));

        Invoice invoice = new Invoice(
                "123",
                "1792049504001",
                "Mi Empresa S.A.",
                "Av. Amazonas, Quito",
                "001",
                "001",
                "000000001",
                "SI",
                "1",
                "Contribuyente Régimen RIMPE",
                "123",
                "NO",
                LocalDate.now(),
                client,
                List.of(line1),
                "01"
        );

        assertEquals(BigDecimal.valueOf(10.00).setScale(2), invoice.getTotalWithoutTax());
        assertEquals(BigDecimal.valueOf(1.50).setScale(2), invoice.getTotalTaxValue());
        assertEquals(BigDecimal.valueOf(11.50).setScale(2), invoice.getTotalAmount());
    }
}
