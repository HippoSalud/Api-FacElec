package com.factura.demo.infrastructure.adapter.out.signature;

import com.factura.demo.application.port.out.signature.DigitalSignatureServicePort;
import com.factura.demo.domain.exception.signature.SignatureProcessingException;
import com.factura.demo.domain.model.signature.DocumentToSign;
import com.factura.demo.domain.model.signature.SignatureContext;
import com.factura.demo.domain.model.signature.SignatureProfile;
import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.enumerations.SignaturePackaging;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.model.SignatureValue;
import eu.europa.esig.dss.model.ToBeSigned;
import eu.europa.esig.dss.pades.PAdESSignatureParameters;
import eu.europa.esig.dss.pades.SignatureImageParameters;
import eu.europa.esig.dss.pades.signature.PAdESService;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import eu.europa.esig.dss.token.Pkcs12SignatureToken;
import eu.europa.esig.dss.validation.CertificateVerifier;
import eu.europa.esig.dss.validation.CommonCertificateVerifier;
import eu.europa.esig.dss.xades.XAdESSignatureParameters;
import eu.europa.esig.dss.xades.signature.XAdESService;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.security.KeyStore;

@Component
public class DssXadesPadesSignatureAdapter implements DigitalSignatureServicePort {

    @Override
    public byte[] signDocument(DocumentToSign document, SignatureContext context, SignatureProfile profile) {
        try (Pkcs12SignatureToken token = new Pkcs12SignatureToken(context.getP12File(), new KeyStore.PasswordProtection(context.getPassword()))) {
            
            DSSPrivateKeyEntry privateKey = token.getKeys().get(0);
            DSSDocument toSignDocument = new InMemoryDocument(document.getContent());

            if (document.getType() == DocumentToSign.DocumentType.XML) {
                return signXml(toSignDocument, token, privateKey, profile);
            } else if (document.getType() == DocumentToSign.DocumentType.PDF) {
                return signPdf(toSignDocument, token, privateKey, context, profile);
            } else {
                throw new SignatureProcessingException("Tipo de documento no soportado para firma");
            }
        } catch (Exception e) {
            throw new SignatureProcessingException("Error interno al firmar el documento con DSS: " + e.getMessage(), e);
        }
    }

    private byte[] signXml(DSSDocument document, Pkcs12SignatureToken token, DSSPrivateKeyEntry privateKey, SignatureProfile profile) {
        XAdESSignatureParameters parameters = new XAdESSignatureParameters();
        
        // Mapeo exacto de los requerimientos del Anexo 14 del SRI:
        // 1. signatureLevel = XAdES_BES -> Incluye SignedProperties con SigningTime y SigningCertificate (CertDigest e IssuerSerial)
        parameters.setSignatureLevel(SignatureLevel.XAdES_BASELINE_B);
        
        // 2. signaturePackaging = ENVELOPED -> Genera un nodo <ds:Signature> dentro del XML original y añade la referencia de transform ENVELOPED.
        parameters.setSignaturePackaging(SignaturePackaging.ENVELOPED);
        
        // 3. digestAlgorithm = SHA1 -> Para <ds:DigestMethod Algorithm="...#sha1"> en todas las referencias (SRI Facturas)
        parameters.setDigestAlgorithm(DigestAlgorithm.SHA1);
        
        // 5. Configurar el certificado para rellenar KeyInfo
        parameters.setSigningCertificate(privateKey.getCertificate());
        parameters.setCertificateChain(privateKey.getCertificateChain());

        // LIMITACIONES DE DSS DOCUMENTADAS SEGÚN EL PROMPT:
        // - DSS genera automáticamente los ds:Reference para SignedProperties, KeyInfo (Certificate) y el Documento.
        // - El orden de los ds:Reference está dictado por las políticas internas de canonización de DSS/Apache Santuario,
        //   y suele coincidir (Properties, KeyInfo, Document), pero no se puede forzar un orden manual arbitrario desde la API pública.
        // - El Id "Signature620397" o similares en el ejemplo del Anexo 14 son dinámicos. DSS generará Ids únicos (UUIDs o secuenciales)
        //   que cumplen con el estándar XAdES. El SRI valida la consistencia de los Ids y referencias, no los valores literales.

        CertificateVerifier cv = new CommonCertificateVerifier();
        XAdESService service = new XAdESService(cv);
        ToBeSigned dataToSign = service.getDataToSign(document, parameters);
        SignatureValue signatureValue = token.sign(dataToSign, parameters.getDigestAlgorithm(), privateKey);
        DSSDocument signedDocument = service.signDocument(document, parameters, signatureValue);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            signedDocument.writeTo(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new SignatureProcessingException("Error escribiendo el XML firmado", e);
        }
    }

    private byte[] signPdf(DSSDocument document, Pkcs12SignatureToken token, DSSPrivateKeyEntry privateKey, SignatureContext context, SignatureProfile profile) {
        PAdESSignatureParameters parameters = new PAdESSignatureParameters();
        
        parameters.setSignatureLevel(SignatureLevel.PAdES_BASELINE_B);
        parameters.setDigestAlgorithm(DigestAlgorithm.SHA1); // Se puede ajustar a SHA-256 si el trámite es otro
        parameters.setSigningCertificate(privateKey.getCertificate());
        parameters.setCertificateChain(privateKey.getCertificateChain());

        // Manejo de coordenadas visuales (origen inferior-izquierdo)
        if (context.getVisualSignaturePage() != null && context.getVisualSignatureX() != null && context.getVisualSignatureY() != null) {
            SignatureImageParameters imageParameters = new SignatureImageParameters();
            eu.europa.esig.dss.pades.SignatureFieldParameters fieldParameters = new eu.europa.esig.dss.pades.SignatureFieldParameters();
            fieldParameters.setPage(context.getVisualSignaturePage());
            fieldParameters.setOriginX(context.getVisualSignatureX());
            fieldParameters.setOriginY(context.getVisualSignatureY());
            imageParameters.setFieldParameters(fieldParameters);
            parameters.setImageParameters(imageParameters);
        }

        CertificateVerifier cv = new CommonCertificateVerifier();
        PAdESService service = new PAdESService(cv);
        ToBeSigned dataToSign = service.getDataToSign(document, parameters);
        SignatureValue signatureValue = token.sign(dataToSign, parameters.getDigestAlgorithm(), privateKey);
        DSSDocument signedDocument = service.signDocument(document, parameters, signatureValue);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            signedDocument.writeTo(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new SignatureProcessingException("Error escribiendo el PDF firmado", e);
        }
    }
}
