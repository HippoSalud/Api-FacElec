package com.factura.demo.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "sri")
public class SriProperties {

    private String environment = "1"; // "1" = Pruebas/Dev, "2" = Producción/Prod
    private boolean sandboxMode = true; // Allow mock signatures & fallback in dev
    private String recepcionUrl = "https://celcer.sri.gob.ec/comprobantes-electronicos-ws/RecepcionComprobantesOffline";
    private String autorizacionUrl = "https://celcer.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline";
    private String anulacionUrl = "https://celcer.sri.gob.ec/comprobantes-electronicos-ws/AnulacionComprobantesOffline";

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public boolean isSandboxMode() {
        return sandboxMode;
    }

    public void setSandboxMode(boolean sandboxMode) {
        this.sandboxMode = sandboxMode;
    }

    public String getRecepcionUrl() {
        return recepcionUrl;
    }

    public void setRecepcionUrl(String recepcionUrl) {
        this.recepcionUrl = recepcionUrl;
    }

    public String getAutorizacionUrl() {
        return autorizacionUrl;
    }

    public void setAutorizacionUrl(String autorizacionUrl) {
        this.autorizacionUrl = autorizacionUrl;
    }

    public String getAnulacionUrl() {
        return anulacionUrl;
    }

    public void setAnulacionUrl(String anulacionUrl) {
        this.anulacionUrl = anulacionUrl;
    }
}
