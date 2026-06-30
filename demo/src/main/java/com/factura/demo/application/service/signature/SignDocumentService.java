package com.factura.demo.application.service.signature;

import com.factura.demo.application.port.in.signature.SignDocumentUseCase;
import com.factura.demo.application.port.out.signature.CertificateValidationPort;
import com.factura.demo.application.port.out.signature.DigitalSignatureServicePort;
import com.factura.demo.domain.model.signature.DocumentToSign;
import com.factura.demo.domain.model.signature.SignatureContext;
import com.factura.demo.domain.model.signature.SignatureProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SignDocumentService implements SignDocumentUseCase {

    private final CertificateValidationPort validationPort;
    private final DigitalSignatureServicePort signaturePort;

    @Override
    public byte[] sign(DocumentToSign document, SignatureContext context) {
        try {
            // 1. Validar el certificado (RUC cruzado, autoridades válidas, vigencia)
            validationPort.validateCertificate(context);

            // 2. Obtener el perfil adecuado (para comprobantes SRI es RSA-SHA1 siempre)
            SignatureProfile profile = SignatureProfile.defaultSriInvoiceProfile();

            // 3. Firmar el documento
            return signaturePort.signDocument(document, context, profile);

        } finally {
            // 4. Limpiar la contraseña del certificado de la memoria como mejor esfuerzo
            if (context != null) {
                context.clearPassword();
            }
        }
    }
}
