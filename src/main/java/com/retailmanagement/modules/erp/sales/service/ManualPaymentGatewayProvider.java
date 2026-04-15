package com.retailmanagement.modules.erp.sales.service;

import com.retailmanagement.modules.erp.sales.entity.SalesInvoice;
import com.retailmanagement.modules.erp.sales.entity.SalesInvoicePaymentRequest;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ManualPaymentGatewayProvider implements PaymentGatewayProvider {

    @Override
    public String providerCode() {
        return "MANUAL";
    }

    @Override
    public String providerName() {
        return "Internal payment link";
    }

    @Override
    public PaymentLinkResult createPaymentLink(PaymentLinkCommand command, SalesInvoice invoice, SalesInvoicePaymentRequest paymentRequest) {
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("providerStatus", "READY");
        payload.put("paymentMode", "INTERNAL_LINK");
        payload.put("invoiceNumber", invoice.getInvoiceNumber());
        payload.put("requestNumber", paymentRequest.getRequestNumber());
        payload.put("requestedAmount", command.requestedAmount());
        payload.put("expiresOn", command.expiresOn());
        payload.put("message", "Internal payment request created. Replace provider later without changing ERP flow.");
        return new PaymentLinkResult(
                providerCode(),
                providerName(),
                paymentRequest.getRequestNumber(),
                "READY",
                "/pay/requests/" + command.internalToken(),
                payload,
                now,
                null
        );
    }

    @Override
    public PaymentStatusResult syncPaymentRequest(SalesInvoice invoice, SalesInvoicePaymentRequest paymentRequest) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("providerStatus", paymentRequest.getProviderStatus() == null ? "READY" : paymentRequest.getProviderStatus());
        payload.put("paymentMode", "INTERNAL_LINK");
        payload.put("invoiceNumber", invoice.getInvoiceNumber());
        payload.put("requestNumber", paymentRequest.getRequestNumber());
        payload.put("message", "Internal/manual provider has no remote sync. Payment completion still depends on receipt posting.");
        return new PaymentStatusResult(
                providerCode(),
                providerName(),
                paymentRequest.getProviderReference(),
                paymentRequest.getProviderStatus() == null ? "READY" : paymentRequest.getProviderStatus(),
                paymentRequest.getPaymentLinkUrl(),
                payload,
                LocalDateTime.now(),
                null
        );
    }
}
