package com.factura.demo.infrastructure.adapter.out.pdf;

import com.factura.demo.application.port.out.PdfGeneratorPort;
import com.factura.demo.domain.model.CommercialDocument;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.krysalis.barcode4j.impl.code128.Code128Bean;
import org.krysalis.barcode4j.output.bitmap.BitmapCanvasProvider;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

@Component
public class ThymeleafPdfGeneratorAdapter implements PdfGeneratorPort {

    private final TemplateEngine templateEngine;

    public ThymeleafPdfGeneratorAdapter(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    @Override
    public byte[] generateRide(CommercialDocument document) {
        Context context = new Context();
        context.setVariable("document", document);
        context.setVariable("emisor", new EmisorDTO(
                document.getEmitterRuc(),
                document.getEmitterCompanyName(),
                document.getEmitterEstablishmentAddress()
        ));
        context.setVariable("cliente", document.getClient());
        
        String barcode = null;
        String accessKeyValue = null;
        String authNumber = null;
        String authDate = null;
        String environment = null;

        if (document instanceof com.factura.demo.domain.model.ElectronicDocument elecDoc) {
            if (elecDoc.getAccessKey() != null) {
                barcode = generateBarcodeBase64(elecDoc.getAccessKey().getValue());
                accessKeyValue = elecDoc.getAccessKey().getValue();
            }
            authNumber = elecDoc.getAuthorizationNumber();
            if (elecDoc.getAuthorizationDate() != null) {
                authDate = elecDoc.getAuthorizationDate().toString();
            }
            environment = elecDoc.getEnvironment();
        }
        
        context.setVariable("barcodeImage", barcode);
        context.setVariable("accessKeyValue", accessKeyValue);
        context.setVariable("authNumber", authNumber);
        context.setVariable("authDate", authDate);
        context.setVariable("environment", environment);

        String htmlContent = templateEngine.process("ride-template", context);

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(htmlContent, null);
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar PDF: " + e.getMessage(), e);
        }
    }

    private String generateBarcodeBase64(String barcodeText) {
        try {
            Code128Bean barcodeGenerator = new Code128Bean();
            barcodeGenerator.setHeight(15f);
            barcodeGenerator.setModuleWidth(0.3);
            barcodeGenerator.setQuietZone(10);
            barcodeGenerator.doQuietZone(true);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            BitmapCanvasProvider canvas = new BitmapCanvasProvider(baos, "image/png", 150, BufferedImage.TYPE_BYTE_BINARY, false, 0);
            barcodeGenerator.generateBarcode(canvas, barcodeText);
            canvas.finish();

            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            return null; // Fallback gracefully if barcode generation fails
        }
    }

    // Helper class to expose simple emisor properties to Thymeleaf
    public static class EmisorDTO {
        private final String ruc;
        private final String companyName;
        private final String address;

        public EmisorDTO(String ruc, String companyName, String address) {
            this.ruc = ruc;
            this.companyName = companyName;
            this.address = address;
        }

        public String getRuc() { return ruc; }
        public String getCompanyName() { return companyName; }
        public String getAddress() { return address; }
    }
}
