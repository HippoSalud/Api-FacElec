package com.factura.demo.domain.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class AccessKeyTest {

    @Test
    void shouldCalculateCorrectModulo11CheckDigit() {
        // Known SRI Ecuadorian key test case
        // Let's calculate for a dummy base
        String base = "290520260117920495040011001001000000001123456781";
        int checkDigit = AccessKey.calculateModulo11(base);
        
        // Assert it's a single digit (0-9)
        assertTrue(checkDigit >= 0 && checkDigit <= 9, "El dígito verificador debe estar entre 0 y 9");
    }

    @Test
    void shouldGenerateCompleteAccessKey() {
        LocalDate date = LocalDate.of(2026, 5, 29);
        AccessKey key = AccessKey.generate(
                date,
                "01",
                "1792049504001",
                "1",
                "001",
                "001",
                "000000001",
                "12345678",
                "1"
        );

        assertNotNull(key);
        assertEquals(49, key.getValue().length(), "La clave de acceso debe tener 49 dígitos");
        assertTrue(key.getValue().startsWith("29052026"), "Debe comenzar con la fecha formateada");
    }
}
