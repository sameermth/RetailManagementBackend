package com.retailmanagement.modules.erp.sales.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.erp.common.ErpSecurityUtils;
import com.retailmanagement.modules.erp.common.constants.ErpDocumentStatuses;
import com.retailmanagement.modules.erp.common.security.ErpAccessGuard;
import com.retailmanagement.modules.erp.sales.dto.ErpSalesDtos;
import com.retailmanagement.modules.erp.sales.dto.ErpSalesResponses;
import com.retailmanagement.modules.erp.sales.entity.CustomerReceiptAllocation;
import com.retailmanagement.modules.erp.sales.entity.SalesInvoice;
import com.retailmanagement.modules.erp.sales.entity.SalesInvoicePaymentRequest;
import com.retailmanagement.modules.erp.sales.repository.CustomerReceiptAllocationRepository;
import com.retailmanagement.modules.erp.sales.repository.SalesInvoicePaymentRequestRepository;
import com.retailmanagement.modules.erp.sales.repository.SalesInvoiceRepository;
import com.retailmanagement.modules.erp.subscription.service.SubscriptionAccessService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class SalesInvoicePaymentRequestService {

    private final SalesInvoicePaymentRequestRepository paymentRequestRepository;
    private final SalesInvoiceRepository salesInvoiceRepository;
    private final CustomerReceiptAllocationRepository customerReceiptAllocationRepository;
    private final ErpAccessGuard accessGuard;
    private final SubscriptionAccessService subscriptionAccessService;
    private final List<PaymentGatewayProvider> paymentGatewayProviders;
    private final ObjectMapper objectMapper;

    @Value("${erp.sales.payments.gateway-provider:MANUAL}")
    private String configuredPaymentGatewayProviderCode;

    @Transactional(readOnly = true)
    public List<SalesInvoicePaymentRequest> listPaymentRequests(Long organizationId) {
        accessGuard.assertOrganizationAccess(organizationId);
        subscriptionAccessService.assertFeature(organizationId, "payments");
        return paymentRequestRepository.findTop100ByOrganizationIdOrderByRequestDateDescIdDesc(organizationId).stream()
                .map(this::synchronizeStatus)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SalesInvoicePaymentRequest> listInvoicePaymentRequests(Long invoiceId) {
        SalesInvoice invoice = requireInvoice(invoiceId);
        return paymentRequestRepository.findBySalesInvoiceIdOrderByRequestDateDescIdDesc(invoice.getId()).stream()
                .map(this::synchronizeStatus)
                .toList();
    }

    @Transactional(readOnly = true)
    public SalesInvoicePaymentRequest getPaymentRequest(Long id) {
        SalesInvoicePaymentRequest paymentRequest = paymentRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sales invoice payment request not found: " + id));
        accessGuard.assertOrganizationAccess(paymentRequest.getOrganizationId());
        accessGuard.assertBranchAccess(paymentRequest.getOrganizationId(), paymentRequest.getBranchId());
        subscriptionAccessService.assertFeature(paymentRequest.getOrganizationId(), "payments");
        return synchronizeStatus(paymentRequest);
    }

    public SalesInvoicePaymentRequest createPaymentRequest(Long organizationId,
                                                           Long branchId,
                                                           Long salesInvoiceId,
                                                           ErpSalesDtos.CreateSalesInvoicePaymentRequestRequest request) {
        accessGuard.assertBranchAccess(organizationId, branchId);
        subscriptionAccessService.assertFeature(organizationId, "payments");
        SalesInvoice invoice = salesInvoiceRepository.findByIdAndOrganizationId(salesInvoiceId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Sales invoice not found: " + salesInvoiceId));
        if (!invoice.getBranchId().equals(branchId)) {
            throw new BusinessException("Sales invoice does not belong to branch " + branchId);
        }
        if (ErpDocumentStatuses.CANCELLED.equals(invoice.getStatus())) {
            throw new BusinessException("Cannot create payment request for a cancelled invoice");
        }
        BigDecimal outstanding = invoiceOutstandingAmount(invoice);
        if (outstanding.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Sales invoice is already fully paid");
        }

        LocalDate requestDate = request.requestDate() == null ? LocalDate.now() : request.requestDate();
        LocalDate dueDate = request.dueDate() == null ? invoice.getDueDate() : request.dueDate();
        LocalDate expiresOn = request.expiresOn() == null ? dueDate : request.expiresOn();
        if (dueDate != null && expiresOn != null && expiresOn.isBefore(dueDate)) {
            throw new BusinessException("Payment request expiry cannot be before due date");
        }
        BigDecimal requestedAmount = request.requestedAmount() == null ? outstanding : request.requestedAmount();
        if (requestedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Requested amount must be greater than zero");
        }
        if (requestedAmount.compareTo(outstanding) > 0) {
            throw new BusinessException("Requested amount cannot exceed invoice outstanding amount");
        }

        SalesInvoicePaymentRequest paymentRequest = new SalesInvoicePaymentRequest();
        paymentRequest.setOrganizationId(organizationId);
        paymentRequest.setBranchId(branchId);
        paymentRequest.setSalesInvoiceId(invoice.getId());
        paymentRequest.setCustomerId(invoice.getCustomerId());
        paymentRequest.setRequestNumber("PAYREQ-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + System.currentTimeMillis());
        paymentRequest.setRequestDate(requestDate);
        paymentRequest.setDueDate(dueDate);
        paymentRequest.setExpiresOn(expiresOn);
        paymentRequest.setRequestedAmount(requestedAmount);
        paymentRequest.setChannel(trimToUpperOrDefault(request.channel(), "LINK"));
        String token = UUID.randomUUID().toString();
        paymentRequest.setPaymentLinkToken(token);
        paymentRequest.setStatus(resolveStatus(invoice, expiresOn));
        paymentRequest.setRemarks(trimToNull(request.remarks()));
        PaymentGatewayProvider provider = resolvePaymentGatewayProvider(request.providerCode());
        PaymentGatewayProvider.PaymentLinkResult paymentLink = provider.createPaymentLink(
                new PaymentGatewayProvider.PaymentLinkCommand(
                        organizationId,
                        branchId,
                        invoice.getId(),
                        invoice.getCustomerId(),
                        invoice.getInvoiceNumber(),
                        paymentRequest.getRequestNumber(),
                        token,
                        requestedAmount,
                        dueDate,
                        expiresOn,
                        paymentRequest.getChannel(),
                        paymentRequest.getRemarks()
                ),
                invoice,
                paymentRequest
        );
        paymentRequest.setProviderCode(paymentLink.providerCode());
        paymentRequest.setProviderReference(trimToNull(paymentLink.providerReference()));
        paymentRequest.setProviderStatus(trimToNull(paymentLink.providerStatus()));
        paymentRequest.setPaymentLinkUrl(trimToNull(paymentLink.paymentLinkUrl()));
        paymentRequest.setProviderPayloadJson(writeJson(paymentLink.providerPayload()));
        paymentRequest.setProviderCreatedAt(paymentLink.providerCreatedAt());
        paymentRequest.setProviderLastSyncedAt(paymentLink.providerCreatedAt());
        return paymentRequestRepository.save(paymentRequest);
    }

    public SalesInvoicePaymentRequest cancelPaymentRequest(Long id, String reason) {
        SalesInvoicePaymentRequest paymentRequest = getPaymentRequest(id);
        if (ErpDocumentStatuses.CANCELLED.equals(paymentRequest.getStatus())) {
            return paymentRequest;
        }
        if (ErpDocumentStatuses.PAID.equals(paymentRequest.getStatus())) {
            throw new BusinessException("Cannot cancel a settled payment request");
        }
        paymentRequest.setStatus(ErpDocumentStatuses.CANCELLED);
        paymentRequest.setCancelledAt(LocalDateTime.now());
        paymentRequest.setCancelledBy(ErpSecurityUtils.currentUserId().orElse(1L));
        paymentRequest.setCancelReason(trimToNull(reason));
        return paymentRequestRepository.save(paymentRequest);
    }

    public void synchronizeInvoiceRequests(Long salesInvoiceId) {
        paymentRequestRepository.findBySalesInvoiceIdOrderByRequestDateDescIdDesc(salesInvoiceId).forEach(this::synchronizeStatus);
    }

    public void synchronizeInvoiceRequestsForCustomer(Long customerId, Long organizationId) {
        paymentRequestRepository.findTop100ByOrganizationIdOrderByRequestDateDescIdDesc(organizationId).stream()
                .filter(request -> customerId.equals(request.getCustomerId()))
                .forEach(this::synchronizeStatus);
    }

    @Transactional(readOnly = true)
    public ErpSalesResponses.SalesInvoicePaymentRequestSummaryResponse buildInvoicePaymentSummary(Long salesInvoiceId) {
        List<SalesInvoicePaymentRequest> requests = paymentRequestRepository.findBySalesInvoiceIdOrderByRequestDateDescIdDesc(salesInvoiceId).stream()
                .map(this::synchronizeStatus)
                .toList();
        if (requests.isEmpty()) {
            return null;
        }
        List<SalesInvoicePaymentRequest> activeRequests = requests.stream()
                .filter(request -> !ErpDocumentStatuses.CANCELLED.equals(request.getStatus()))
                .toList();
        SalesInvoicePaymentRequest latest = requests.getFirst();
        return new ErpSalesResponses.SalesInvoicePaymentRequestSummaryResponse(
                latest.getStatus(),
                activeRequests.size(),
                latest.getRequestDate(),
                latest.getExpiresOn(),
                latest.getPaymentLinkUrl(),
                latest.getProviderCode(),
                latest.getProviderStatus()
        );
    }

    @Transactional(readOnly = true)
    public List<ErpSalesResponses.SalesInvoicePaymentRequestResponse> toResponses(List<SalesInvoicePaymentRequest> requests) {
        return requests.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ErpSalesResponses.PaymentGatewayProviderResponse> listGatewayProviders() {
        String configuredProvider = normalizedProviderCode(null);
        return paymentGatewayProviders.stream()
                .map(provider -> new ErpSalesResponses.PaymentGatewayProviderResponse(
                        provider.providerCode(),
                        provider.providerName(),
                        provider.providerCode().equalsIgnoreCase(configuredProvider),
                        provider.simulated(),
                        provider.supportsStatusSync()
                ))
                .toList();
    }

    public SalesInvoicePaymentRequest syncPaymentRequestProviderStatus(Long id) {
        SalesInvoicePaymentRequest paymentRequest = getPaymentRequest(id);
        if (ErpDocumentStatuses.CANCELLED.equals(paymentRequest.getStatus()) || ErpDocumentStatuses.PAID.equals(paymentRequest.getStatus())) {
            return paymentRequest;
        }
        SalesInvoice invoice = salesInvoiceRepository.findById(paymentRequest.getSalesInvoiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Sales invoice not found: " + paymentRequest.getSalesInvoiceId()));
        PaymentGatewayProvider provider = resolvePaymentGatewayProvider(paymentRequest.getProviderCode());
        PaymentGatewayProvider.PaymentStatusResult statusResult = provider.syncPaymentRequest(invoice, paymentRequest);
        paymentRequest.setProviderCode(statusResult.providerCode());
        paymentRequest.setProviderReference(trimToNull(statusResult.providerReference()));
        paymentRequest.setProviderStatus(trimToNull(statusResult.providerStatus()));
        if (statusResult.paymentLinkUrl() != null && !statusResult.paymentLinkUrl().isBlank()) {
            paymentRequest.setPaymentLinkUrl(statusResult.paymentLinkUrl());
        }
        paymentRequest.setProviderPayloadJson(writeJson(statusResult.providerPayload()));
        paymentRequest.setProviderLastSyncedAt(statusResult.providerLastSyncedAt());
        return paymentRequestRepository.save(paymentRequest);
    }

    @Transactional(readOnly = true)
    public ErpSalesResponses.SalesInvoicePaymentRequestResponse toResponse(SalesInvoicePaymentRequest paymentRequest) {
        SalesInvoice invoice = salesInvoiceRepository.findById(paymentRequest.getSalesInvoiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Sales invoice not found: " + paymentRequest.getSalesInvoiceId()));
        BigDecimal allocatedAmount = invoiceAllocatedAmount(invoice.getId());
        BigDecimal outstandingAmount = invoice.getTotalAmount().subtract(allocatedAmount).max(BigDecimal.ZERO);
        return new ErpSalesResponses.SalesInvoicePaymentRequestResponse(
                paymentRequest.getId(),
                paymentRequest.getOrganizationId(),
                paymentRequest.getBranchId(),
                paymentRequest.getSalesInvoiceId(),
                paymentRequest.getCustomerId(),
                invoice.getInvoiceNumber(),
                paymentRequest.getRequestNumber(),
                paymentRequest.getRequestDate(),
                paymentRequest.getDueDate(),
                paymentRequest.getExpiresOn(),
                paymentRequest.getRequestedAmount(),
                allocatedAmount,
                outstandingAmount,
                paymentRequest.getProviderCode(),
                resolvePaymentGatewayProvider(paymentRequest.getProviderCode()).providerName(),
                paymentRequest.getProviderReference(),
                paymentRequest.getProviderStatus(),
                paymentRequest.getChannel(),
                paymentRequest.getPaymentLinkToken(),
                paymentRequest.getPaymentLinkUrl(),
                paymentRequest.getStatus(),
                paymentRequest.getProviderCreatedAt(),
                paymentRequest.getProviderLastSyncedAt(),
                readJsonMap(paymentRequest.getProviderPayloadJson()),
                paymentRequest.getLastSentAt(),
                paymentRequest.getCancelledAt(),
                paymentRequest.getCancelReason(),
                paymentRequest.getRemarks()
        );
    }

    private SalesInvoice requireInvoice(Long invoiceId) {
        SalesInvoice invoice = salesInvoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Sales invoice not found: " + invoiceId));
        accessGuard.assertOrganizationAccess(invoice.getOrganizationId());
        accessGuard.assertBranchAccess(invoice.getOrganizationId(), invoice.getBranchId());
        subscriptionAccessService.assertFeature(invoice.getOrganizationId(), "payments");
        return invoice;
    }

    private SalesInvoicePaymentRequest synchronizeStatus(SalesInvoicePaymentRequest paymentRequest) {
        if (ErpDocumentStatuses.CANCELLED.equals(paymentRequest.getStatus())) {
            return paymentRequest;
        }
        SalesInvoice invoice = salesInvoiceRepository.findById(paymentRequest.getSalesInvoiceId()).orElse(null);
        if (invoice == null) {
            return paymentRequest;
        }
        String resolvedStatus = resolveStatus(invoice, paymentRequest.getExpiresOn());
        if (!resolvedStatus.equals(paymentRequest.getStatus())) {
            paymentRequest.setStatus(resolvedStatus);
            return paymentRequestRepository.save(paymentRequest);
        }
        return paymentRequest;
    }

    private String resolveStatus(SalesInvoice invoice, LocalDate expiresOn) {
        BigDecimal allocated = invoiceAllocatedAmount(invoice.getId());
        BigDecimal outstanding = invoice.getTotalAmount().subtract(allocated).max(BigDecimal.ZERO);
        if (outstanding.compareTo(BigDecimal.ZERO) <= 0 || ErpDocumentStatuses.PAID.equals(invoice.getStatus())) {
            return ErpDocumentStatuses.PAID;
        }
        if (expiresOn != null && expiresOn.isBefore(LocalDate.now())) {
            return ErpDocumentStatuses.EXPIRED;
        }
        if (allocated.compareTo(BigDecimal.ZERO) > 0 || ErpDocumentStatuses.PARTIALLY_PAID.equals(invoice.getStatus())) {
            return ErpDocumentStatuses.PARTIALLY_PAID;
        }
        return ErpDocumentStatuses.REQUESTED;
    }

    private PaymentGatewayProvider resolvePaymentGatewayProvider(String requestedProviderCode) {
        String providerCode = normalizedProviderCode(requestedProviderCode);
        return paymentGatewayProviders.stream()
                .filter(provider -> provider.providerCode().equalsIgnoreCase(providerCode))
                .findFirst()
                .orElseGet(() -> paymentGatewayProviders.stream()
                        .filter(provider -> "MANUAL".equalsIgnoreCase(provider.providerCode()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Manual payment gateway provider is missing")));
    }

    private String normalizedProviderCode(String requestedProviderCode) {
        String candidate = trimToNull(requestedProviderCode);
        String configured = trimToNull(configuredPaymentGatewayProviderCode);
        return (candidate == null ? (configured == null ? "MANUAL" : configured) : candidate).toUpperCase(Locale.ROOT);
    }

    private BigDecimal invoiceAllocatedAmount(Long salesInvoiceId) {
        return customerReceiptAllocationRepository.findBySalesInvoiceIdOrderByIdAsc(salesInvoiceId).stream()
                .map(CustomerReceiptAllocation::getAllocatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal invoiceOutstandingAmount(SalesInvoice invoice) {
        return invoice.getTotalAmount().subtract(invoiceAllocatedAmount(invoice.getId())).max(BigDecimal.ZERO);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String trimToUpperOrDefault(String value, String defaultValue) {
        String trimmed = trimToNull(value);
        return trimmed == null ? defaultValue : trimmed.toUpperCase();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readJsonMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("Unable to deserialize payment gateway payload");
        }
    }

    private String writeJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("Unable to serialize payment gateway payload");
        }
    }
}
