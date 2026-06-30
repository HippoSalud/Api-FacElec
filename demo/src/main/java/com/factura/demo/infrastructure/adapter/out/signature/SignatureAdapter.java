package com.factura.demo.infrastructure.adapter.out.signature;

import com.factura.demo.application.port.out.SignaturePort;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.crypto.dom.DOMStructure;
import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;

public class SignatureAdapter implements SignaturePort {

    private final com.factura.demo.infrastructure.config.SriProperties sriProperties;

    public SignatureAdapter(com.factura.demo.infrastructure.config.SriProperties sriProperties) {
        this.sriProperties = sriProperties;
    }

    @Override
    public String sign(String unsignedXml, byte[] p12Certificate, String p12Password) {
        if (p12Certificate == null || p12Certificate.length == 0 || p12Password == null || p12Password.isBlank()) {
            if (sriProperties.isSandboxMode()) {
                // Graceful Mock fallback mode when certificate is not supplied (extremely useful for development & sandbox tests)
                return unsignedXml.replace("</factura>", "  <!-- SIGNATURE MOCK (XAdES-BES Simulation) -->\n</factura>");
            } else {
                throw new IllegalArgumentException("La firma electrónica .p12 y contraseña son obligatorias en ambiente de producción.");
            }
        }

        try {
            // 1. Load Certificate and Private Key from PKCS#12
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(new ByteArrayInputStream(p12Certificate), p12Password.toCharArray());

            String alias = null;
            Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String a = aliases.nextElement();
                if (keyStore.isKeyEntry(a)) {
                    alias = a;
                    break;
                }
            }

            if (alias == null) {
                throw new IllegalArgumentException("No se encontró una clave privada en el certificado .p12");
            }

            PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, p12Password.toCharArray());
            X509Certificate certificate = (X509Certificate) keyStore.getCertificate(alias);

            // 2. Parse unsigned XML to DOM Document
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new ByteArrayInputStream(unsignedXml.getBytes("UTF-8")));

            // Configure schema validation for Root element
            Element rootElement = doc.getDocumentElement();
            rootElement.setIdAttribute("id", true);

            // 3. Generate Random IDs for XAdES standard structures
            String signatureId = "Signature-" + UUID.randomUUID();
            String signedPropertiesId = "SignedProperties-" + UUID.randomUUID();
            String keyInfoId = "KeyInfo-" + UUID.randomUUID();

            // 4. Calculate Certificate Digest (SHA-1) for XAdES properties
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] derCert = certificate.getEncoded();
            byte[] certDigestValue = md.digest(derCert);
            String certDigestB64 = java.util.Base64.getEncoder().encodeToString(certDigestValue);

            // 5. Build XAdES-BES DOM Structure manually to comply with SRI specifications
            String xadesNs = "http://uri.etsi.org/01903/v1.3.2#";
            String dsigNs = "http://www.w3.org/2000/09/xmldsig#";

            Element qualifyingProperties = doc.createElementNS(xadesNs, "xades:QualifyingProperties");
            qualifyingProperties.setAttribute("Target", "#" + signatureId);

            Element signedProperties = doc.createElementNS(xadesNs, "xades:SignedProperties");
            signedProperties.setAttribute("Id", signedPropertiesId);
            signedProperties.setIdAttribute("Id", true); // Vital so XMLDSig reference resolution works

            Element signedSignatureProperties = doc.createElementNS(xadesNs, "xades:SignedSignatureProperties");

            // Signing Time
            Element signingTime = doc.createElementNS(xadesNs, "xades:SigningTime");
            String isoDate = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            signingTime.setTextContent(isoDate);
            signedSignatureProperties.appendChild(signingTime);

            // Signing Certificate
            Element signingCertificate = doc.createElementNS(xadesNs, "xades:SigningCertificate");
            Element cert = doc.createElementNS(xadesNs, "xades:Cert");

            Element certDigest = doc.createElementNS(xadesNs, "xades:CertDigest");
            Element digestMethod = doc.createElementNS(dsigNs, "DigestMethod");
            digestMethod.setAttribute("Algorithm", "http://www.w3.org/2000/09/xmldsig#sha1");
            Element digestValue = doc.createElementNS(dsigNs, "DigestValue");
            digestValue.setTextContent(certDigestB64);
            certDigest.appendChild(digestMethod);
            certDigest.appendChild(digestValue);
            cert.appendChild(certDigest);

            Element issuerSerial = doc.createElementNS(xadesNs, "xades:IssuerSerial");
            Element x509IssuerName = doc.createElementNS(dsigNs, "X509IssuerName");
            x509IssuerName.setTextContent(certificate.getIssuerX500Principal().getName());
            Element x509SerialNumber = doc.createElementNS(dsigNs, "X509SerialNumber");
            x509SerialNumber.setTextContent(certificate.getSerialNumber().toString());
            issuerSerial.appendChild(x509IssuerName);
            issuerSerial.appendChild(x509SerialNumber);
            cert.appendChild(issuerSerial);

            signingCertificate.appendChild(cert);
            signedSignatureProperties.appendChild(signingCertificate);
            signedProperties.appendChild(signedSignatureProperties);
            qualifyingProperties.appendChild(signedProperties);

            // 6. Setup XML Signature Factory
            XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");

            // Reference to Root Document (id="comprobante")
            Reference documentRef = fac.newReference(
                    "#comprobante",
                    fac.newDigestMethod(DigestMethod.SHA1, null),
                    Collections.singletonList(fac.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null)),
                    null,
                    null
            );

            // Reference to SignedProperties (XAdES standard requirements)
            Reference signedPropertiesRef = fac.newReference(
                    "#" + signedPropertiesId,
                    fac.newDigestMethod(DigestMethod.SHA1, null),
                    Collections.emptyList(),
                    "http://uri.etsi.org/01903#SignedProperties",
                    null
            );

            // Build SignedInfo with both references
            SignedInfo si = fac.newSignedInfo(
                    fac.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE, (C14NMethodParameterSpec) null),
                    fac.newSignatureMethod(SignatureMethod.RSA_SHA1, null),
                    Arrays.asList(documentRef, signedPropertiesRef)
            );

            // Build KeyInfo
            KeyInfoFactory kif = fac.getKeyInfoFactory();
            X509Data xd = kif.newX509Data(Arrays.asList(certificate.getSubjectX500Principal().getName(), certificate));
            KeyInfo ki = kif.newKeyInfo(Collections.singletonList(xd), keyInfoId);

            // Wrap DOM QualifyingProperties inside XAdES Object
            DOMStructure xadesStructure = new DOMStructure(qualifyingProperties);
            XMLObject xadesObject = fac.newXMLObject(Collections.singletonList(xadesStructure), null, null, null);

            // 7. Execute Digital Signature
            DOMSignContext dsc = new DOMSignContext(privateKey, rootElement);
            XMLSignature signature = fac.newXMLSignature(si, ki, Collections.singletonList(xadesObject), signatureId, null);
            signature.sign(dsc);

            // 8. Output XML Document as String
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer trans = tf.newTransformer();
            StringWriter writer = new StringWriter();
            trans.transform(new DOMSource(doc), new StreamResult(writer));

            return writer.toString();

        } catch (Exception e) {
            throw new RuntimeException("Error durante el proceso de firma digital XAdES-BES: " + e.getMessage(), e);
        }
    }
}
