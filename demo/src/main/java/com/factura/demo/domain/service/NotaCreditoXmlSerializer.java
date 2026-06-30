package com.factura.demo.domain.service;

import com.factura.demo.domain.model.InvoiceLine;
import com.factura.demo.domain.model.NotaCredito;
import com.factura.demo.domain.model.Tax;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public final class NotaCreditoXmlSerializer {

    public static String serialize(NotaCredito notaCredito) {
        if (notaCredito.getAccessKey() == null) {
            throw new IllegalStateException("La Nota de Crédito debe tener una Clave de Acceso asociada antes de serializar a XML");
        }

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        // Nota de Crédito versión 1.1.0
        xml.append("<notaCredito id=\"comprobante\" version=\"1.1.0\">\n");
        
        // infoTributaria (codDoc = 04 para Nota de Crédito)
        BaseXmlSerializer.appendInfoTributaria(xml, notaCredito, "04");

        // infoNotaCredito
        xml.append("  <infoNotaCredito>\n");
        xml.append("    <fechaEmision>").append(notaCredito.getEmissionDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("</fechaEmision>\n");
        xml.append("    <dirEstablecimiento>").append(BaseXmlSerializer.escapeXml(notaCredito.getEmitterEstablishmentAddress())).append("</dirEstablecimiento>\n");
        xml.append("    <tipoIdentificacionComprador>").append(notaCredito.getClient().getIdentificationType()).append("</tipoIdentificacionComprador>\n");
        xml.append("    <razonSocialComprador>").append(BaseXmlSerializer.escapeXml(notaCredito.getClient().getName())).append("</razonSocialComprador>\n");
        xml.append("    <identificacionComprador>").append(notaCredito.getClient().getIdentification()).append("</identificacionComprador>\n");
        if (notaCredito.getSpecialContributorResolution() != null && !notaCredito.getSpecialContributorResolution().isBlank()) {
            xml.append("    <contribuyenteEspecial>").append(BaseXmlSerializer.escapeXml(notaCredito.getSpecialContributorResolution())).append("</contribuyenteEspecial>\n");
        }
        xml.append("    <obligadoContabilidad>").append(notaCredito.getObligationToKeepBooks()).append("</obligadoContabilidad>\n");
        xml.append("    <codDocModificado>").append(notaCredito.getModifiedDocumentType()).append("</codDocModificado>\n");
        xml.append("    <numDocModificado>").append(notaCredito.getModifiedDocumentId()).append("</numDocModificado>\n");
        xml.append("    <fechaEmisionDocSustento>").append(notaCredito.getModifiedDocumentEmissionDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("</fechaEmisionDocSustento>\n");
        xml.append("    <totalSinImpuestos>").append(notaCredito.getTotalWithoutTax()).append("</totalSinImpuestos>\n");
        xml.append("    <valorModificacion>").append(notaCredito.getTotalAmount()).append("</valorModificacion>\n");
        xml.append("    <motivo>").append(BaseXmlSerializer.escapeXml(notaCredito.getModificationReason())).append("</motivo>\n");

        // totalConImpuestos (Aggregated by tax code & percentage code)
        xml.append("    <totalConImpuestos>\n");
        Map<String, TaxAggregation> taxAggregations = aggregateTaxes(notaCredito);
        for (TaxAggregation agg : taxAggregations.values()) {
            xml.append("      <totalImpuesto>\n");
            xml.append("        <codigo>").append(agg.code).append("</codigo>\n");
            xml.append("        <codigoPorcentaje>").append(agg.percentageCode).append("</codigoPorcentaje>\n");
            xml.append("        <baseImponible>").append(agg.base.setScale(2, BigDecimal.ROUND_HALF_UP)).append("</baseImponible>\n");
            xml.append("        <valor>").append(agg.value.setScale(2, BigDecimal.ROUND_HALF_UP)).append("</valor>\n");
            xml.append("      </totalImpuesto>\n");
        }
        xml.append("    </totalConImpuestos>\n");
        xml.append("  </infoNotaCredito>\n");

        // detalles
        xml.append("  <detalles>\n");
        for (InvoiceLine line : notaCredito.getLines()) {
            xml.append("    <detalle>\n");
            xml.append("      <codigoInterno>").append(BaseXmlSerializer.escapeXml(line.getMainCode())).append("</codigoInterno>\n");
            xml.append("      <descripcion>").append(BaseXmlSerializer.escapeXml(line.getDescription())).append("</descripcion>\n");
            xml.append("      <cantidad>").append(line.getQuantity().setScale(6, BigDecimal.ROUND_HALF_UP)).append("</cantidad>\n");
            xml.append("      <precioUnitario>").append(line.getUnitPrice().setScale(6, BigDecimal.ROUND_HALF_UP)).append("</precioUnitario>\n");
            xml.append("      <descuento>").append(line.getDiscount().setScale(2, BigDecimal.ROUND_HALF_UP)).append("</descuento>\n");
            xml.append("      <precioTotalSinImpuesto>").append(line.getTotalPriceWithoutTax().setScale(2, BigDecimal.ROUND_HALF_UP)).append("</precioTotalSinImpuesto>\n");
            
            // impuestos -> impuesto
            xml.append("      <impuestos>\n");
            for (Tax tax : line.getTaxes()) {
                xml.append("        <impuesto>\n");
                xml.append("          <codigo>").append(tax.getCode()).append("</codigo>\n");
                xml.append("          <codigoPorcentaje>").append(tax.getPercentageCode()).append("</codigoPorcentaje>\n");
                xml.append("          <tarifa>").append(tax.getRate().setScale(2, BigDecimal.ROUND_HALF_UP)).append("</tarifa>\n");
                xml.append("          <baseImponible>").append(tax.getTaxableBase().setScale(2, BigDecimal.ROUND_HALF_UP)).append("</baseImponible>\n");
                xml.append("          <valor>").append(tax.getValue().setScale(2, BigDecimal.ROUND_HALF_UP)).append("</valor>\n");
                xml.append("        </impuesto>\n");
            }
            xml.append("      </impuestos>\n");
            xml.append("    </detalle>\n");
        }
        xml.append("  </detalles>\n");

        xml.append("</notaCredito>");
        return xml.toString();
    }

    private static Map<String, TaxAggregation> aggregateTaxes(NotaCredito notaCredito) {
        Map<String, TaxAggregation> aggMap = new HashMap<>();
        for (InvoiceLine line : notaCredito.getLines()) {
            for (Tax tax : line.getTaxes()) {
                String key = tax.getCode() + "-" + tax.getPercentageCode();
                aggMap.compute(key, (k, v) -> {
                    if (v == null) {
                        return new TaxAggregation(tax.getCode(), tax.getPercentageCode(), tax.getTaxableBase(), tax.getValue());
                    } else {
                        v.base = v.base.add(tax.getTaxableBase());
                        v.value = v.value.add(tax.getValue());
                        return v;
                    }
                });
            }
        }
        return aggMap;
    }

    private static class TaxAggregation {
        String code;
        String percentageCode;
        BigDecimal base;
        BigDecimal value;

        TaxAggregation(String code, String percentageCode, BigDecimal base, BigDecimal value) {
            this.code = code;
            this.percentageCode = percentageCode;
            this.base = base;
            this.value = value;
        }
    }
}
