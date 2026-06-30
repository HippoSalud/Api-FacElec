package com.factura.demo.domain.model.signature;

import lombok.Builder;
import lombok.Getter;

/**
 * Encapsula la configuración algorítmica de la firma según el perfil requerido (ej. comprobantes electrónicos SRI).
 * Facilita el Principio de Abierto/Cerrado (OCP) si el SRI introduce nuevos perfiles en el futuro (ej. Anexo CRS usa SHA-256).
 */
@Getter
@Builder
public class SignatureProfile {
    private final SignatureAlgorithm algorithm;
    private final DigestAlgorithm digest;
    private final String xadesSchemaVersion;
    private final SignaturePackaging packaging;

    public enum SignatureAlgorithm {
        RSA_SHA1,
        RSA_SHA256
    }

    public enum DigestAlgorithm {
        SHA1,
        SHA256
    }
    
    public enum SignaturePackaging {
        ENVELOPED,
        DETACHED
    }

    /**
     * Perfil estándar oficial para comprobantes electrónicos del SRI de Ecuador (Facturas, Notas de Crédito, etc.).
     */
    public static SignatureProfile defaultSriInvoiceProfile() {
        return SignatureProfile.builder()
                .algorithm(SignatureAlgorithm.RSA_SHA1)
                .digest(DigestAlgorithm.SHA1)
                .xadesSchemaVersion("1.3.2")
                .packaging(SignaturePackaging.ENVELOPED)
                .build();
    }
}
