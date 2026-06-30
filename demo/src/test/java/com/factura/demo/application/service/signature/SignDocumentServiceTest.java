package com.factura.demo.application.service.signature;

import com.factura.demo.application.port.out.signature.CertificateValidationPort;
import com.factura.demo.application.port.out.signature.DigitalSignatureServicePort;
import com.factura.demo.domain.model.signature.DocumentToSign;
import com.factura.demo.domain.model.signature.SignatureContext;
import com.factura.demo.domain.model.signature.SignatureProfile;
import com.factura.demo.infrastructure.adapter.out.signature.DssXadesPadesSignatureAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SignDocumentServiceTest {

    @Mock
    private CertificateValidationPort validationPort;

    // Aquí usamos la implementación real de DSS para la prueba de integración
    private DigitalSignatureServicePort signaturePort;

    private SignDocumentService signDocumentService;

    @BeforeEach
    void setUp() {
        signaturePort = new DssXadesPadesSignatureAdapter();
        signDocumentService = new SignDocumentService(validationPort, signaturePort);
    }

    /**
     * Prueba de Integración Local
     * 
     * INSTRUCCIONES:
     * 1. Coloca un archivo XML válido del SRI en la carpeta 'src/test/resources/' (ej. 'factura-test.xml')
     * 2. Coloca tu archivo certificado real en 'src/test/resources/' (ej. 'firma.p12')
     * 3. Cambia la contraseña en la prueba.
     * 4. Quita la anotación @Disabled y ejecuta la prueba en tu IDE.
     */
    @Test
    void testFirmarDocumentoXmlReal() throws Exception {
        // 1. Cargar el XML de prueba
        byte[] xmlBytes = Files.readAllBytes(Paths.get("src/test/resources/factura-test.xml"));
        DocumentToSign document = DocumentToSign.builder()
                .content(xmlBytes)
                .type(DocumentToSign.DocumentType.XML)
                .build();

        // 2. Cargar el Certificado P12
        byte[] p12Bytes = Files.readAllBytes(Paths.get("src/test/resources/firma.p12"));
        SignatureContext context = SignatureContext.builder()
                .p12File(p12Bytes)
                .password("Pepe123".toCharArray()) // <- Cambiar por contraseña real
                // .expectedRuc("1790000000001") // Opcional
                .build();

        // Evitar que la validación del mock falle (asumimos que el cert es válido)
        doNothing().when(validationPort).validateCertificate(any(SignatureContext.class));

        // 3. Ejecutar la firma
        byte[] signedXmlBytes = signDocumentService.sign(document, context);

        // 4. Verificaciones
        assertNotNull(signedXmlBytes);
        String signedXmlStr = new String(signedXmlBytes);
        
        // Verificar que el XML resultante contiene la firma (ds:Signature)
        assertTrue(signedXmlStr.contains("ds:Signature"));
        assertTrue(signedXmlStr.contains("ds:X509Certificate"));
        
        // 5. Opcional: Guardar el resultado en disco para visualizarlo
        Files.write(Paths.get("target/factura-firmada.xml"), signedXmlBytes);
        System.out.println("Documento firmado exitosamente. Revisa la carpeta target/");
    }
}
