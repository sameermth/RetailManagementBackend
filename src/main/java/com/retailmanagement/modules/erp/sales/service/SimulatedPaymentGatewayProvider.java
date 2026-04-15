package com.retailmanagement.modules.erp.sales.service;

import com.retailmanagement.modules.erp.sales.entity.SalesInvoice;
import com.retailmanagement.modules.erp.sales.entity.SalesInvoicePaymentRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class SimulatedPaymentGatewayProvider implements PaymentGatewayProvider {

    @Override
    public String providerCode() {
        return "SIMULATED";
    }

    @Override
    public String providerName() {
        return "Simulated payment gateway";
    }

    @Override
    public boolean simulated() {
        return true;
    }

    @Override
    public PaymentLinkResult createPaymentLink(PaymentLinkCommand command, SalesInvoice invoice, SalesInvoicePaymentRequest paymentRequest) {
        LocalDateTime now = LocalDateTime.now();
        String providerReference = "SIM-PAY-" + now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("providerStatus", "LINK_CREATED");
        payload.put("sandbox", true);
        payload.put("providerReference", providerReference);
        payload.put("invoiceNumber", invoice.getInvoiceNumber());
        payload.put("requestNumber", paymentRequest.getRequestNumber());
        payload.put("requestedAmount", command.requestedAmount());
        payload.put("message", "Simulated payment link created successfully.");
        return new PaymentLinkResult(
                providerCode(),
                providerName(),
                providerReference,
                "LINK_CREATED",
                "https://sandbox-pay.retail.example/checkout/" + providerReference,
                payload,
                now,
                null
        );
    }

    @Override
    public PaymentStatusResult syncPaymentRequest(SalesInvoice invoice, SalesInvoicePaymentRequest paymentRequest) {
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("providerStatus", "LINK_ACTIVE");
        payload.put("sandbox", true);
        payload.put("providerReference", paymentRequest.getProviderReference());
        payload.put("invoiceNumber", invoice.getInvoiceNumber());
        payload.put("requestNumber", paymentRequest.getRequestNumber());
        payload.put("message", "Simulated provider sync completed.");
        return new PaymentStatusResult(
                providerCode(),
                providerName(),
                paymentRequest.getProviderReference(),
                "LINK_ACTIVE",
                paymentRequest.getPaymentLinkUrl(),
                payload,
                now,
                null
        );
    }
}
