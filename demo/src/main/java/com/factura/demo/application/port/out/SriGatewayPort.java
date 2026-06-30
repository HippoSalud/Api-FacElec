package com.factura.demo.application.port.out;

import java.time.LocalDate;

public interface SriGatewayPort {

    /**
     * Sends the signed XML to SRI Recepcion web service.
     *
     * @param signedXml The signed XML content.
     * @param environment "1" for Test, "2" for Production.
     * @return boolean true if successfully received, false or throws exception if rejected/returned.
     */
    boolean send(String signedXml, String environment);

    /**
     * Queries the SRI Autorizacion web service for the given access key.
     *
     * @param accessKey The 49-digit access key.
     * @param environment "1" for Test, "2" for Production.
     * @return SriAuthorizationResult containing status, authorization number, and date.
     */
    SriAuthorizationResult checkAuthorization(String accessKey, String environment);

    record SriAuthorizationResult(
        boolean authorized,
        String authorizationNumber,
        LocalDate authorizationDate,
        String errorMessage
    ) {}

    /**
     * Requests the annulment of an authorized document.
     *
     * @param accessKey The access key of the document.
     * @param authorizationNumber The authorization number of the document.
     * @param signedAnnulmentXml The signed XML for the annulment request.
     * @param environment "1" for Test, "2" for Production.
     * @return true if annulled successfully, false otherwise.
     */
    boolean requestAnnulment(String accessKey, String authorizationNumber, String signedAnnulmentXml, String environment);
}
