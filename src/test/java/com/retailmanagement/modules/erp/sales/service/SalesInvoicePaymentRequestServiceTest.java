package com.retailmanagement.modules.erp.sales.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.retailmanagement.modules.erp.common.security.ErpAccessGuard;
import com.retailmanagement.modules.erp.sales.dto.ErpSalesDtos;
import com.retailmanagement.modules.erp.sales.entity.SalesInvoice;
import com.retailmanagement.modules.erp.sales.entity.SalesInvoicePaymentRequest;
import com.retailmanagement.modules.erp.sales.repository.CustomerReceiptAllocationRepository;
import com.retailmanagement.modules.erp.sales.repository.SalesInvoicePaymentRequestRepository;
import com.retailmanagement.modules.erp.sales.repository.SalesInvoiceRepository;
import com.retailmanagement.modules.erp.subscription.service.SubscriptionAccessService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SalesInvoicePaymentRequestServiceTest {

    @Mock private SalesInvoicePaymentRequestRepository paymentRequestRepository;
    @Mock private SalesInvoiceRepository salesInvoiceRepository;
    @Mock private CustomerReceiptAllocationRepository customerReceiptAllocationRepository;
    @Mock private ErpAccessGuard accessGuard;
    @Mock private SubscriptionAccessService subscriptionAccessService;

    @Test
    void createPaymentRequest_usesSimulatedProviderWhenRequested() {
        SalesInvoicePaymentRequestService service = service();
        injectConfiguredProvider(service, "MANUAL");

        SalesInvoice invoice = invoice();
        when(salesInvoiceRepository.findByIdAndOrganizationId(54L, 33L)).thenReturn(Optional.of(invoice));
        when(accessGuard.assertBranchAccess(33L, 73L)).thenReturn(73L);
        when(paymentRequestRepository.save(any(SalesInvoicePaymentRequest.class))).thenAnswer(invocation -> {
            SalesInvoicePaymentRequest saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        SalesInvoicePaymentRequest created = service.createPaymentRequest(
                33L,
                73L,
                54L,
                new ErpSalesDtos.CreateSalesInvoicePaymentRequestRequest(
                        33L,
                        73L,
                        LocalDate.of(2026, 4, 15),
                        LocalDate.of(2026, 4, 20),
                        LocalDate.of(2026, 4, 20),
                        new BigDecimal("205.32"),
                        "SIMULATED",
                        "LINK",
                        "Test gateway request"
                )
        );

        assertEquals("SIMULATED", created.getProviderCode());
        assertEquals("LINK_CREATED", created.getProviderStatus());
        assertTrue(created.getPaymentLinkUrl().contains("sandbox-pay.retail.example"));
        assertTrue(created.getProviderPayloadJson().contains("LINK_CREATED"));
    }

    @Test
    void syncPaymentRequestProviderStatus_updatesGatewayMetadata() {
        SalesInvoicePaymentRequestService service = service();
        injectConfiguredProvider(service, "SIMULATED");

        SalesInvoice invoice = invoice();
        SalesInvoicePaymentRequest paymentRequest = new SalesInvoicePaymentRequest();
        paymentRequest.setId(11L);
        paymentRequest.setOrganizationId(33L);
        paymentRequest.setBranchId(73L);
        paymentRequest.setSalesInvoiceId(54L);
        paymentRequest.setCustomerId(57L);
        paymentRequest.setRequestNumber("PAYREQ-1");
        paymentRequest.setRequestDate(LocalDate.of(2026, 4, 15));
        paymentRequest.setRequestedAmount(new BigDecimal("205.32"));
        paymentRequest.setProviderCode("SIMULATED");
        paymentRequest.setProviderReference("SIM-PAY-20260415010101");
        paymentRequest.setProviderStatus("LINK_CREATED");
        paymentRequest.setPaymentLinkToken("abc");
        paymentRequest.setPaymentLinkUrl("https://sandbox-pay.retail.example/checkout/SIM-PAY-20260415010101");
        paymentRequest.setStatus("REQUESTED");

        when(paymentRequestRepository.findById(11L)).thenReturn(Optional.of(paymentRequest));
        when(salesInvoiceRepository.findById(54L)).thenReturn(Optional.of(invoice));
        when(accessGuard.assertOrganizationAccess(33L)).thenReturn(33L);
        when(accessGuard.assertBranchAccess(33L, 73L)).thenReturn(73L);
        when(paymentRequestRepository.save(any(SalesInvoicePaymentRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SalesInvoicePaymentRequest synced = service.syncPaymentRequestProviderStatus(11L);

        assertEquals("SIMULATED", synced.getProviderCode());
        assertEquals("LINK_ACTIVE", synced.getProviderStatus());
        assertTrue(synced.getProviderPayloadJson().contains("LINK_ACTIVE"));
    }

    private SalesInvoicePaymentRequestService service() {
        return new SalesInvoicePaymentRequestService(
                paymentRequestRepository,
                salesInvoiceRepository,
                customerReceiptAllocationRepository,
                accessGuard,
                subscriptionAccessService,
                List.of(new ManualPaymentGatewayProvider(), new SimulatedPaymentGatewayProvider()),
                new ObjectMapper().registerModule(new JavaTimeModule())
        );
    }

    private SalesInvoice invoice() {
        SalesInvoice invoice = new SalesInvoice();
        invoice.setId(54L);
        invoice.setOrganizationId(33L);
        invoice.setBranchId(73L);
        invoice.setCustomerId(57L);
        invoice.setInvoiceNumber("INV-54");
        invoice.setDueDate(LocalDate.of(2026, 4, 20));
        invoice.setStatus("POSTED");
        invoice.setTotalAmount(new BigDecimal("205.32"));
        return invoice;
    }

    private static void injectConfiguredProvider(SalesInvoicePaymentRequestService service, String value) {
        try {
            var field = SalesInvoicePaymentRequestService.class.getDeclaredField("configuredPaymentGatewayProviderCode");
            field.setAccessible(true);
            field.set(service, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
