package com.factura.demo.domain.service;

import com.factura.demo.domain.model.Invoice;
import com.factura.demo.domain.model.InvoiceLine;
import com.factura.demo.domain.model.Tax;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Pure Java serializer to build the SRI Ecuador XML structure for an invoice.
 */
public final class InvoiceXmlSerializer {

    public static String serialize(Invoice invoice) {
        if (invoice.getAccessKey() == null) {
            throw new IllegalStateException("La factura debe tener una Clave de Acceso asociada antes de serializar a XML");
        }

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<factura id=\"comprobante\" version=\"1.1.0\">\n");
        
        // infoTributaria
        BaseXmlSerializer.appendInfoTributaria(xml, invoice, "01");

        // infoFactura
        xml.append("  <infoFactura>\n");
        xml.append("    <fechaEmision>").append(invoice.getEmissionDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("</fechaEmision>\n");
        xml.append("    <dirEstablecimiento>").append(BaseXmlSerializer.escapeXml(invoice.getEmitterEstablishmentAddress())).append("</dirEstablecimiento>\n");
        if (invoice.getSpecialContributorResolution() != null && !invoice.getSpecialContributorResolution().isBlank()) {
            xml.append("    <contribuyenteEspecial>").append(BaseXmlSerializer.escapeXml(invoice.getSpecialContributorResolution())).append("</contribuyenteEspecial>\n");
        }
        xml.append("    <obligadoContabilidad>").append(invoice.getObligationToKeepBooks()).append("</obligadoContabilidad>\n");
        xml.append("    <tipoIdentificacionComprador>").append(invoice.getClient().getIdentificationType()).append("</tipoIdentificacionComprador>\n");
        xml.append("    <razonSocialComprador>").append(BaseXmlSerializer.escapeXml(invoice.getClient().getName())).append("</razonSocialComprador>\n");
        xml.append("    <identificacionComprador>").append(invoice.getClient().getIdentification()).append("</identificacionComprador>\n");
        xml.append("    <totalSinImpuestos>").append(invoice.getTotalWithoutTax()).append("</totalSinImpuestos>\n");
        xml.append("    <totalDescuento>").append(invoice.getTotalDiscount()).append("</totalDescuento>\n");
        
        // totalConImpuestos (Aggregated by tax code & percentage code)
        xml.append("    <totalConImpuestos>\n");
        Map<String, TaxAggregation> taxAggregations = aggregateTaxes(invoice);
        for (TaxAggregation agg : taxAggregations.values()) {
            xml.append("      <totalImpuesto>\n");
            xml.append("        <codigo>").append(agg.code).append("</codigo>\n");
            xml.append("        <codigoPorcentaje>").append(agg.percentageCode).append("</codigoPorcentaje>\n");
            xml.append("        <baseImponible>").append(agg.base.setScale(2, BigDecimal.ROUND_HALF_UP)).append("</baseImponible>\n");
            xml.append("        <valor>").append(agg.value.setScale(2, BigDecimal.ROUND_HALF_UP)).append("</valor>\n");
            xml.append("      </totalImpuesto>\n");
        }
        xml.append("    </totalConImpuestos>\n");

        xml.append("    <propina>0.00</propina>\n");
        xml.append("    <importeTotal>").append(invoice.getTotalAmount()).append("</importeTotal>\n");
        xml.append("    <moneda>DOLAR</moneda>\n");
        
        // pagos
        xml.append("    <pagos>\n");
        xml.append("      <pago>\n");
        xml.append("        <formaPago>").append(invoice.getPaymentMethod()).append("</formaPago>\n");
        xml.append("        <total>").append(invoice.getTotalAmount()).append("</total>\n");
        xml.append("      </pago>\n");
        xml.append("    </pagos>\n");
        xml.append("  </infoFactura>\n");

        // detalles
        xml.append("  <detalles>\n");
        for (InvoiceLine line : invoice.getLines()) {
            xml.append("    <detalle>\n");
            xml.append("      <codigoPrincipal>").append(BaseXmlSerializer.escapeXml(line.getMainCode())).append("</codigoPrincipal>\n");
            xml.append("      <descripcion>").append(BaseXmlSerializer.escapeXml(line.getDescription())).append("</descripcion>\n");
            xml.append("      <cantidad>").append(line.getQuantity().setScale(6, BigDecimal.ROUND_HALF_UP)).append("</cantidad>\n");
            xml.append("      <precioUnitario>").append(line.getUnitPrice().setScale(6, BigDecimal.ROUND_HALF_UP)).append("</precioUnitario>\n");
            xml.append("      <descuento>").append(line.getDiscount().setScale(2, BigDecimal.ROUND_HALF_UP)).append("</descuento>\n");
            xml.append("      <precioTotalSinImpuesto>").append(line.getTotalPriceWithoutTax().setScale(2, BigDecimal.ROUND_HALF_UP)).append("</precioTotalSinImpuesto>\n");
            
            // detalles -> detalle -> impuestos -> impuesto
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

        // infoAdicional (Optional extension point)
        // xml.append("  <infoAdicional>\n");
        // xml.append("  </infoAdicional>\n");

        xml.append("</factura>");
        return xml.toString();
    }

    private static Map<String, TaxAggregation> aggregateTaxes(Invoice invoice) {
        Map<String, TaxAggregation> aggMap = new HashMap<>();
        for (InvoiceLine line : invoice.getLines()) {
            for (Tax tax : line.getTaxes()) {
                String key = tax.getCode() + "_" + tax.getPercentageCode();
                aggMap.computeIfAbsent(key, k -> new TaxAggregation(tax.getCode(), tax.getPercentageCode()))
                      .add(tax.getTaxableBase(), tax.getValue());
            }
        }
        return aggMap;
    }

    private static class TaxAggregation {
        final String code;
        final String percentageCode;
        BigDecimal base = BigDecimal.ZERO;
        BigDecimal value = BigDecimal.ZERO;

        TaxAggregation(String code, String percentageCode) {
            this.code = code;
            this.percentageCode = percentageCode;
        }

        void add(BigDecimal basePart, BigDecimal valuePart) {
            this.base = this.base.add(basePart);
            this.value = this.value.add(valuePart);
        }
    }
}
