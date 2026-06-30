package com.factura.demo.domain.model.signature;

import lombok.Builder;
import lombok.Getter;

import java.util.Arrays;

/**
 * Contexto de firma que encapsula el certificado en bytes y su contraseña.
 * La contraseña se maneja como char[] para permitir su limpieza manual de la memoria
 * lo más pronto posible después de usarla (mejor esfuerzo).
 */
@Getter
@Builder
public class SignatureContext {
    private final byte[] p12File;
    private final char[] password;
    private final String expectedRuc;

    // Opcional: parámetros visuales para el PDF (coordenadas estándar PDF origen inferior-izquierdo)
    private final Integer visualSignaturePage;
    private final Float visualSignatureX;
    private final Float visualSignatureY;

    /**
     * Limpia la contraseña de la memoria sobre-escribiendo sus caracteres con nulos.
     * Esto es un esfuerzo de seguridad, pero no garantiza que no haya copias en el heap (por GC u optimizaciones JIT).
     */
    public void clearPassword() {
        if (password != null) {
            Arrays.fill(password, '\0');
        }
    }
}
