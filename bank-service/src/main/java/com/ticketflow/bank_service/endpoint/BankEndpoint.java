package com.ticketflow.bank_service.endpoint;

import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;
import org.w3c.dom.Element;

@Endpoint
public class BankEndpoint {

    private static final String NAMESPACE_URI =
            "http://ticketflow.com/bank";

    @PayloadRoot(
            namespace = NAMESPACE_URI,
            localPart = "AuthorizePaymentRequest"
    )
    @ResponsePayload
    public Element authorizePayment(@RequestPayload Element request) {
        return createResponse(
                "AuthorizePaymentResponse",
                true,
                "TX-" + System.currentTimeMillis(),
                "Paiement autorisé"
        );
    }

    @PayloadRoot(
            namespace = NAMESPACE_URI,
            localPart = "RefundPaymentRequest"
    )
    @ResponsePayload
    public Element refundPayment(@RequestPayload Element request) {
        return createResponse(
                "RefundPaymentResponse",
                true,
                "REF-" + System.currentTimeMillis(),
                "Remboursement effectué"
        );
    }

    private Element createResponse(
            String responseName,
            boolean success,
            String transactionId,
            String message) {

        try {
            var document = javax.xml.parsers.DocumentBuilderFactory
                    .newInstance()
                    .newDocumentBuilder()
                    .newDocument();

            Element response = document.createElementNS(
                    NAMESPACE_URI,
                    "tns:" + responseName
            );

            document.appendChild(response);

            Element successElement = document.createElementNS(
                    NAMESPACE_URI,
                    "success"
            );
            successElement.setTextContent(
                    String.valueOf(success)
            );
            response.appendChild(successElement);

            Element transactionElement = document.createElementNS(
                    NAMESPACE_URI,
                    "transactionId"
            );
            transactionElement.setTextContent(transactionId);
            response.appendChild(transactionElement);

            Element messageElement = document.createElementNS(
                    NAMESPACE_URI,
                    "message"
            );
            messageElement.setTextContent(message);
            response.appendChild(messageElement);

            return response;

        } catch (Exception e) {
            throw new RuntimeException(
                    "Erreur lors de la création de la réponse SOAP",
                    e
            );
        }
    }
}