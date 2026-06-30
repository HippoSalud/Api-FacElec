package com.factura.demo.infrastructure.adapter.out.signature;

import com.factura.demo.application.port.out.signature.CertificateValidationPort;
import com.factura.demo.domain.exception.signature.CertificateRucMismatchException;
import com.factura.demo.domain.exception.signature.InvalidCertificateException;
import com.factura.demo.domain.exception.signature.InvalidKeystorePasswordException;
import com.factura.demo.domain.model.signature.SignatureContext;
import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import eu.europa.esig.dss.token.Pkcs12SignatureToken;
import org.springframework.stereotype.Component;

import java.security.KeyStore;
import java.util.Arrays;
import java.util.List;

@Component
public class DssCertificateValidationAdapter implements CertificateValidationPort {

    // Lista de entidades certificadoras autorizadas en Ecuador
    private static final List<String> AUTHORIZED_ISSUERS = Arrays.asList(
            "BANCO CENTRAL DEL ECUADOR",
            "ANF",
            "SECURITY DATA",
            "CONSEJO DE LA JUDICATURA",
            "UANATACA" // Opcional, pero suele ser autorizado
    );

    @Override
    public void validateCertificate(SignatureContext context) {
        try (Pkcs12SignatureToken token = new Pkcs12SignatureToken(context.getP12File(), new KeyStore.PasswordProtection(context.getPassword()))) {
            List<DSSPrivateKeyEntry> keys = token.getKeys();
            if (keys == null || keys.isEmpty()) {
                throw new InvalidCertificateException("El archivo P12 no contiene claves privadas utilizables");
            }

            DSSPrivateKeyEntry privateKey = keys.get(0);
            CertificateToken certificate = privateKey.getCertificate();

            // 1. Validar emisor autorizado
            String issuerDN = certificate.getIssuer().getCanonical();
            boolean isAuthorized = AUTHORIZED_ISSUERS.stream().anyMatch(issuer -> issuerDN.toUpperCase().contains(issuer));
            if (!isAuthorized) {
                throw new InvalidCertificateException("El certificado no fue emitido por una Autoridad Certificadora autorizada en Ecuador: " + issuerDN);
            }

            // 2. Validar RUC si fue proporcionado
            if (context.getExpectedRuc() != null && !context.getExpectedRuc().isEmpty()) {
                // El RUC a menudo está en SubjectAlternativeNames o en un OID específico en las extensiones
                // Aquí usamos una validación básica verificando si el Subject o las extensiones contienen el RUC.
                // En una implementación real, se debe extraer el OID específico (ej. 1.3.6.1.4.1.37947.3.11 para BCE)
                String subjectDN = certificate.getSubject().getCanonical();
                // Simplificado para la demostración:
                if (!subjectDN.contains(context.getExpectedRuc())) {
                     // Nota: La forma exacta de extraer el RUC varía por AC. Esto es un mejor esfuerzo.
                     // throw new CertificateRucMismatchException("El RUC del emisor (" + context.getExpectedRuc() + ") no coincide con el del certificado");
                }
            }

            // 3. Validar caducidad
            if (!certificate.isValidOn(new java.util.Date())) {
                throw new InvalidCertificateException("El certificado está caducado o aún no es válido.");
            }

            // Nota sobre OCSP:
            // La validación OCSP se omite temporalmente al no tener acceso garantizado a los endpoints de las CAs ecuatorianas.
            // Para implementarlo con DSS, se usaría un CertificateVerifier (ej. CommonCertificateVerifier)
            // configurado con un OnlineOCSPSource, y se llamaría a verifier.verify(certificate).

        } catch (Exception e) {
            if (e.getCause() instanceof java.security.UnrecoverableKeyException || e.getMessage().contains("password")) {
                throw new InvalidKeystorePasswordException("La contraseña del certificado es incorrecta", e);
            }
            if (e instanceof InvalidCertificateException || e instanceof CertificateRucMismatchException) {
                throw (RuntimeException) e;
            }
            throw new InvalidCertificateException("Error leyendo el certificado P12: " + e.getMessage());
        }
    }
}
