package com.retailmanagement.modules.erp.sales.service;

import com.retailmanagement.modules.erp.sales.entity.SalesInvoice;
import com.retailmanagement.modules.erp.sales.entity.SalesInvoicePaymentRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

public interface PaymentGatewayProvider {

    String providerCode();

    String providerName();

    default boolean simulated() {
        return false;
    }

    default boolean supportsStatusSync() {
        return true;
    }

    PaymentLinkResult createPaymentLink(PaymentLinkCommand command, SalesInvoice invoice, SalesInvoicePaymentRequest paymentRequest);

    PaymentStatusResult syncPaymentRequest(SalesInvoice invoice, SalesInvoicePaymentRequest paymentRequest);

    record PaymentLinkCommand(
            Long organizationId,
            Long branchId,
            Long salesInvoiceId,
            Long customerId,
            String invoiceNumber,
            String requestNumber,
            String internalToken,
            BigDecimal requestedAmount,
            LocalDate dueDate,
            LocalDate expiresOn,
            String channel,
            String remarks
    ) {}

    record PaymentLinkResult(
            String providerCode,
            String providerName,
            String providerReference,
            String providerStatus,
            String paymentLinkUrl,
            Map<String, Object> providerPayload,
            LocalDateTime providerCreatedAt,
            String errorMessage
    ) {}

    record PaymentStatusResult(
            String providerCode,
            String providerName,
            String providerReference,
            String providerStatus,
            String paymentLinkUrl,
            Map<String, Object> providerPayload,
            LocalDateTime providerLastSyncedAt,
            String errorMessage
    ) {}
}
