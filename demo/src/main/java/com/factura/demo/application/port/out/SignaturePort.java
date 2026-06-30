package com.factura.demo.application.port.out;

public interface SignaturePort {

    /**
     * Signs the raw XML document using XAdES-BES standard with a PKCS#12 signature file.
     *
     * @param unsignedXml The raw, unsigned XML content.
     * @param p12Certificate Binary content of the .p12 certificate file.
     * @param p12Password Password for the .p12 key store.
     * @return The signed XML string.
     */
    String sign(String unsignedXml, byte[] p12Certificate, String p12Password);
}
