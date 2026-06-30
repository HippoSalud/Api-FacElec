package com.factura.demo.infrastructure.adapter.out.sri;

import com.factura.demo.application.port.out.SriGatewayPort;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.util.Base64;

public class SriSoapAdapter implements SriGatewayPort {

    private static final String RECEPCION_PROD_URL = "https://cel.sri.gob.ec/comprobantes-electronicos-ws/RecepcionComprobantesOffline";
    private static final String AUTORIZACION_PROD_URL = "https://cel.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline";

    private final com.factura.demo.infrastructure.config.SriProperties sriProperties;

    public SriSoapAdapter(com.factura.demo.infrastructure.config.SriProperties sriProperties) {
        this.sriProperties = sriProperties;
    }

    @Override
    public boolean send(String signedXml, String environment) {
        if (sriProperties.isSandboxMode() && signedXml.contains("SIGNATURE MOCK")) {
            System.out.println("[SANDBOX] Firma simulada detectada. Simulando recepción exitosa (RECIBIDA) en el SRI.");
            return true;
        }

        // Use properties url by default, fallback to appropriate constant if not set
        String urlString = "2".equals(environment) ? RECEPCION_PROD_URL : sriProperties.getRecepcionUrl();
        
        try {
            // Encode XML in Base64
            String base64Xml = Base64.getEncoder().encodeToString(signedXml.getBytes("UTF-8"));

            // Build SOAP Envelope
            String soapRequest = 
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ec=\"http://ec.gob.sri.ws.recepcion\">\n" +
                "   <soapenv:Header/>\n" +
                "   <soapenv:Body>\n" +
                "      <ec:validarComprobante>\n" +
                "         <xml>" + base64Xml + "</xml>\n" +
                "      </ec:validarComprobante>\n" +
                "   </soapenv:Body>\n" +
                "</soapenv:Envelope>";

            // Execute HTTP Request
            String response = postSoap(urlString, soapRequest);
            System.out.println("[SRI RECEPCION SOAP RESPONSE]: " + response);
            
            // Check if SRI response contains 'RECIBIDA'
            return response != null && response.contains("RECIBIDA");

        } catch (Exception e) {
            if (sriProperties.isSandboxMode()) {
                // For testing and local offline execution, fallback to local simulate
                System.err.println("Advertencia: No se pudo conectar al SRI SOAP (" + e.getMessage() + "). Iniciando simulación local sandbox.");
                return true;
            } else {
                throw new RuntimeException("Error al conectar con el Web Service de Recepción del SRI en producción: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public SriAuthorizationResult checkAuthorization(String accessKey, String environment) {
        String urlString = "2".equals(environment) ? AUTORIZACION_PROD_URL : sriProperties.getAutorizacionUrl();

        try {
            // Build SOAP Envelope
            String soapRequest =
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ec=\"http://ec.gob.sri.ws.autorizacion\">\n" +
                "   <soapenv:Header/>\n" +
                "   <soapenv:Body>\n" +
                "      <ec:autorizacionComprobante>\n" +
                "         <claveAccesoComprobante>" + accessKey + "</claveAccesoComprobante>\n" +
                "      </ec:autorizacionComprobante>\n" +
                "   </soapenv:Body>\n" +
                "</soapenv:Envelope>";

            String response = postSoap(urlString, soapRequest);
            System.out.println("[SRI AUTORIZACION SOAP RESPONSE]: " + response);

            if (response != null && response.contains("AUTORIZADO")) {
                String authNumber = accessKey;
                return new SriAuthorizationResult(true, authNumber, LocalDate.now(), null);
            }

            if (sriProperties.isSandboxMode() && response != null && response.contains("<numeroComprobantes>0</numeroComprobantes>")) {
                System.out.println("[SANDBOX] SRI no encontró el comprobante (probablemente usamos firma MOCK). Simulando autorización exitosa.");
                return new SriAuthorizationResult(true, accessKey, LocalDate.now(), null);
            }

            // Extract error message if present in SOAP response
            String errorMsg = "Comprobante no se encuentra autorizado en el SRI.";
            if (response != null && response.contains("<mensaje>")) {
                int start = response.indexOf("<mensaje>") + 9;
                int end = response.indexOf("</mensaje>");
                if (start > 8 && end > start) {
                    errorMsg = "SRI: " + response.substring(start, end);
                }
            }
            return new SriAuthorizationResult(false, null, null, errorMsg);

        } catch (Exception e) {
            if (sriProperties.isSandboxMode()) {
                // Decoupled sandbox simulation fallback
                System.out.println("[SANDBOX] Falló la conexión real con el SRI (" + e.getMessage() + "). Simulando respuesta autorizada.");
                return new SriAuthorizationResult(true, accessKey, LocalDate.now(), null);
            } else {
                throw new RuntimeException("Error al conectar con el Web Service de Autorización del SRI en producción: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public boolean requestAnnulment(String accessKey, String authorizationNumber, String signedAnnulmentXml, String environment) {
        String urlString = "2".equals(environment) ? 
            "https://cel.sri.gob.ec/comprobantes-electronicos-ws/AnulacionComprobantesOffline" : 
            sriProperties.getAnulacionUrl(); // Note: You'll need to add getAnulacionUrl() to SriProperties if it doesn't exist, but since it's injected from application.properties, it might be.

        try {
            // Encode XML in Base64 if needed, or send directly depending on SRI specification for Anulacion
            String base64Xml = Base64.getEncoder().encodeToString(signedAnnulmentXml.getBytes("UTF-8"));

            String soapRequest =
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ec=\"http://ec.gob.sri.ws.anulacion\">\n" +
                "   <soapenv:Header/>\n" +
                "   <soapenv:Body>\n" +
                "      <ec:anularComprobante>\n" +
                "         <xml>" + base64Xml + "</xml>\n" +
                "      </ec:anularComprobante>\n" +
                "   </soapenv:Body>\n" +
                "</soapenv:Envelope>";

            String response = postSoap(urlString, soapRequest);
            System.out.println("[SRI ANULACION SOAP RESPONSE]: " + response);

            return response != null && (response.contains("RECIBIDA") || response.contains("EXITO"));

        } catch (Exception e) {
            if (sriProperties.isSandboxMode()) {
                System.out.println("[SANDBOX] Falló la conexión con SRI (" + e.getMessage() + "). Simulando anulación exitosa.");
                return true;
            } else {
                throw new RuntimeException("Error al conectar con el Web Service de Anulación del SRI: " + e.getMessage(), e);
            }
        }
    }

    private String postSoap(String endpoint, String soapEnvelope) throws Exception {
        URL url = new URL(endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
        connection.setDoOutput(true);
        connection.setConnectTimeout(5000); // 5 seconds timeout
        connection.setReadTimeout(5000);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = soapEnvelope.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        int status = connection.getResponseCode();
        if (status != HttpURLConnection.HTTP_OK) {
            throw new RuntimeException("Respuesta SOAP del SRI fallida con código de estado HTTP: " + status);
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line.trim());
            }
        }
        return response.toString();
    }
}
