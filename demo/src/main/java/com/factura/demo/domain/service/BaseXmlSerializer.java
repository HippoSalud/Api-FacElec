package com.factura.demo.domain.service;

import com.factura.demo.domain.model.ElectronicDocument;

public final class BaseXmlSerializer {

    public static void appendInfoTributaria(StringBuilder xml, ElectronicDocument doc, String codDoc) {
        xml.append("  <infoTributaria>\n");
        xml.append("    <ambiente>").append(doc.getEnvironment()).append("</ambiente>\n");
        xml.append("    <tipoEmision>1</tipoEmision>\n");
        xml.append("    <razonSocial>").append(escapeXml(doc.getEmitterCompanyName())).append("</razonSocial>\n");
        xml.append("    <nombreComercial>").append(escapeXml(doc.getEmitterCompanyName())).append("</nombreComercial>\n");
        xml.append("    <ruc>").append(doc.getEmitterRuc()).append("</ruc>\n");
        xml.append("    <claveAcceso>").append(doc.getAccessKey().getValue()).append("</claveAcceso>\n");
        xml.append("    <codDoc>").append(codDoc).append("</codDoc>\n");
        xml.append("    <estab>").append(doc.getEstablishmentCode()).append("</estab>\n");
        xml.append("    <ptoEmi>").append(doc.getEmissionPointCode()).append("</ptoEmi>\n");
        xml.append("    <secuencial>").append(doc.getSequential()).append("</secuencial>\n");
        xml.append("    <dirMatriz>").append(escapeXml(doc.getEmitterEstablishmentAddress())).append("</dirMatriz>\n");
        
        if (doc.getRetentionAgent() != null && !doc.getRetentionAgent().isBlank()) {
            xml.append("    <agenteRetencion>").append(escapeXml(doc.getRetentionAgent())).append("</agenteRetencion>\n");
        }
        if (doc.getContributorRegime() != null && !doc.getContributorRegime().isBlank()) {
            xml.append("    <contribuyenteRimpe>").append(escapeXml(doc.getContributorRegime())).append("</contribuyenteRimpe>\n");
        }
        xml.append("  </infoTributaria>\n");
    }

    public static String escapeXml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&apos;");
    }
}
