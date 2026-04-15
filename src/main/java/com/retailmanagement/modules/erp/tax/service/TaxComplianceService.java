package com.retailmanagement.modules.erp.tax.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.erp.common.security.ErpAccessGuard;
import com.retailmanagement.modules.erp.foundation.entity.Branch;
import com.retailmanagement.modules.erp.foundation.entity.Organization;
import com.retailmanagement.modules.erp.foundation.repository.BranchRepository;
import com.retailmanagement.modules.erp.foundation.repository.OrganizationRepository;
import com.retailmanagement.modules.erp.party.entity.Customer;
import com.retailmanagement.modules.erp.party.repository.CustomerRepository;
import com.retailmanagement.modules.erp.sales.entity.SalesInvoice;
import com.retailmanagement.modules.erp.sales.entity.SalesInvoiceLine;
import com.retailmanagement.modules.erp.sales.repository.SalesInvoiceLineRepository;
import com.retailmanagement.modules.erp.sales.repository.SalesInvoiceRepository;
import com.retailmanagement.modules.erp.tax.dto.TaxDtos;
import com.retailmanagement.modules.erp.tax.entity.TaxComplianceDocument;
import com.retailmanagement.modules.erp.tax.repository.TaxComplianceDocumentRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class TaxComplianceService {

    private static final String SOURCE_TYPE_SALES_INVOICE = "SALES_INVOICE";
    private static final String DOCUMENT_TYPE_E_INVOICE = "E_INVOICE";
    private static final String DOCUMENT_TYPE_E_WAY_BILL = "E_WAY_BILL";

    private final ErpAccessGuard accessGuard;
    private final SalesInvoiceRepository salesInvoiceRepository;
    private final SalesInvoiceLineRepository salesInvoiceLineRepository;
    private final CustomerRepository customerRepository;
    private final OrganizationRepository organizationRepository;
    private final BranchRepository branchRepository;
    private final TaxComplianceDocumentRepository taxComplianceDocumentRepository;
    private final List<GstComplianceProvider> complianceProviders;
    private final ObjectMapper objectMapper;

    @Value("${erp.tax.integration.compliance-provider:DISABLED}")
    private String configuredComplianceProviderCode;

    @Transactional(readOnly = true)
    public List<TaxDtos.TaxComplianceDocumentSummaryResponse> listInvoiceDocuments(Long invoiceId) {
        SalesInvoice invoice = salesInvoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Sales invoice not found: " + invoiceId));
        accessGuard.assertOrganizationAccess(invoice.getOrganizationId());
        return taxComplianceDocumentRepository
                .findByOrganizationIdAndSourceTypeAndSourceIdOrderByIdDesc(invoice.getOrganizationId(), SOURCE_TYPE_SALES_INVOICE, invoiceId)
                .stream()
                .map(this::toSummary)
                .toList();
    }

    public TaxDtos.TaxComplianceDocumentResponse createEinvoiceDraft(Long invoiceId, TaxDtos.TaxComplianceDraftRequest request) {
        return createDraft(invoiceId, DOCUMENT_TYPE_E_INVOICE, request);
    }

    public TaxDtos.TaxComplianceDocumentResponse createEwayBillDraft(Long invoiceId, TaxDtos.TaxComplianceDraftRequest request) {
        return createDraft(invoiceId, DOCUMENT_TYPE_E_WAY_BILL, request);
    }

    @Transactional(readOnly = true)
    public TaxDtos.TaxComplianceDocumentResponse getDocument(Long documentId) {
        TaxComplianceDocument document = requireDocument(documentId);
        return toDocumentResponse(document);
    }

    public TaxDtos.TaxComplianceDocumentResponse submitDocument(Long documentId) {
        TaxComplianceDocument document = requireDocument(documentId);
        if (!Boolean.TRUE.equals(document.getEligibleForSubmission())) {
            throw new BusinessException("Resolve GST compliance blockers before submission");
        }
        if ("GENERATED".equalsIgnoreCase(document.getStatus())) {
            return toDocumentResponse(document);
        }

        GstComplianceProvider provider = resolveComplianceProvider();
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = readJsonMap(document.getPayloadJson());
        GstComplianceProvider.SubmissionResult submission = provider.submit(document.getDocumentType(), payload, document);

        document.setProviderCode(submission.providerCode());
        document.setStatus(submission.documentStatus());
        document.setSubmittedAt(LocalDateTime.now());
        document.setLastSyncedAt(LocalDateTime.now());
        document.setExternalReference(submission.externalReference());
        document.setAcknowledgementNumber(submission.acknowledgementNumber());
        document.setAcknowledgementDateTime(submission.acknowledgementDateTime());
        document.setResponseJson(writeJson(submission.responsePayload()));
        document.setErrorMessage(submission.errorMessage());
        document = taxComplianceDocumentRepository.save(document);
        return toDocumentResponse(document);
    }

    public TaxDtos.TaxComplianceDocumentResponse syncDocumentStatus(Long documentId) {
        TaxComplianceDocument document = requireDocument(documentId);
        GstComplianceProvider provider = resolveComplianceProvider();
        GstComplianceProvider.SyncResult syncResult = provider.syncStatus(document.getDocumentType(), document);

        document.setProviderCode(syncResult.providerCode());
        document.setStatus(syncResult.documentStatus());
        document.setLastSyncedAt(LocalDateTime.now());
        if (syncResult.externalReference() != null) {
            document.setExternalReference(syncResult.externalReference());
        }
        if (syncResult.acknowledgementNumber() != null) {
            document.setAcknowledgementNumber(syncResult.acknowledgementNumber());
        }
        if (syncResult.acknowledgementDateTime() != null) {
            document.setAcknowledgementDateTime(syncResult.acknowledgementDateTime());
        }
        document.setResponseJson(writeJson(syncResult.responsePayload()));
        document.setErrorMessage(syncResult.errorMessage());
        document = taxComplianceDocumentRepository.save(document);
        return toDocumentResponse(document);
    }

    private TaxDtos.TaxComplianceDocumentResponse createDraft(Long invoiceId, String documentType, TaxDtos.TaxComplianceDraftRequest request) {
        SalesInvoice invoice = salesInvoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Sales invoice not found: " + invoiceId));
        accessGuard.assertOrganizationAccess(invoice.getOrganizationId());

        Organization organization = organizationRepository.findById(invoice.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + invoice.getOrganizationId()));
        Branch branch = branchRepository.findByIdAndOrganizationId(invoice.getBranchId(), invoice.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + invoice.getBranchId()));
        Customer customer = customerRepository.findByIdAndOrganizationId(invoice.getCustomerId(), invoice.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + invoice.getCustomerId()));
        List<SalesInvoiceLine> lines = salesInvoiceLineRepository.findBySalesInvoiceIdOrderByIdAsc(invoiceId);
        if (lines.isEmpty()) {
            throw new BusinessException("Cannot create GST compliance draft without invoice lines");
        }

        List<String> warnings = buildWarnings(invoice, customer, lines, documentType);
        boolean eligible = warnings.stream().noneMatch(this::isBlockingWarning);
        Map<String, Object> payload = buildPayload(invoice, organization, branch, customer, lines, documentType, request, eligible, warnings);

        TaxComplianceDocument document = new TaxComplianceDocument();
        document.setOrganizationId(invoice.getOrganizationId());
        document.setBranchId(invoice.getBranchId());
        document.setSourceType(SOURCE_TYPE_SALES_INVOICE);
        document.setSourceId(invoice.getId());
        document.setDocumentType(documentType);
        document.setProviderCode(resolveComplianceProvider().providerCode());
        document.setStatus(eligible ? "DRAFT" : "BLOCKED");
        document.setEligibleForSubmission(eligible);
        document.setPayloadJson(writeJson(payload));
        document.setWarningJson(writeJson(warnings));
        document.setGeneratedAt(LocalDateTime.now());
        document.setErrorMessage(eligible ? null : "Resolve GST compliance blockers before submission");

        TaxComplianceDocument saved = taxComplianceDocumentRepository.save(document);
        return toDocumentResponse(saved);
    }

    private List<String> buildWarnings(SalesInvoice invoice, Customer customer, List<SalesInvoiceLine> lines, String documentType) {
        List<String> warnings = new ArrayList<>();
        if (!"POSTED".equalsIgnoreCase(invoice.getStatus())) {
            warnings.add("BLOCKING: Only posted invoices should move to GST compliance submission.");
        }
        if (invoice.getSellerGstin() == null || invoice.getSellerGstin().isBlank()) {
            warnings.add("BLOCKING: Seller GST registration is missing on the invoice.");
        }
        if (invoice.getPlaceOfSupplyStateCode() == null || invoice.getPlaceOfSupplyStateCode().isBlank()) {
            warnings.add("BLOCKING: Place of supply state code is missing on the invoice.");
        }
        if (DOCUMENT_TYPE_E_INVOICE.equals(documentType) && (customer.getGstin() == null || customer.getGstin().isBlank())) {
            warnings.add("Customer GSTIN is missing. B2C invoices may not require e-invoice depending on turnover and rules.");
        }
        if (lines.stream().anyMatch(line -> line.getHsnSnapshot() == null || line.getHsnSnapshot().isBlank())) {
            warnings.add("One or more invoice lines do not have HSN snapshot values.");
        }
        return warnings;
    }

    private boolean isBlockingWarning(String warning) {
        return warning != null && warning.startsWith("BLOCKING:");
    }

    private Map<String, Object> buildPayload(
            SalesInvoice invoice,
            Organization organization,
            Branch branch,
            Customer customer,
            List<SalesInvoiceLine> lines,
            String documentType,
            TaxDtos.TaxComplianceDraftRequest request,
            boolean eligible,
            List<String> warnings
    ) {
        GstComplianceProvider complianceProvider = resolveComplianceProvider();
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("documentType", documentType);
        payload.put("sourceType", SOURCE_TYPE_SALES_INVOICE);
        payload.put("sourceId", invoice.getId());
        payload.put("providerCode", complianceProvider.providerCode());
        payload.put("providerName", complianceProvider.providerName());
        payload.put("eligibleForSubmission", eligible);
        payload.put("warnings", warnings);
        payload.put("generatedAt", LocalDateTime.now());

        payload.put("seller", mapOf(
                "organizationId", organization.getId(),
                "organizationCode", organization.getCode(),
                "legalName", firstNonBlank(organization.getLegalName(), organization.getName()),
                "tradeName", organization.getName(),
                "gstin", invoice.getSellerGstin(),
                "phone", organization.getPhone(),
                "email", organization.getEmail(),
                "branchName", branch.getName(),
                "branchCode", branch.getCode(),
                "addressLine1", branch.getAddressLine1(),
                "addressLine2", branch.getAddressLine2(),
                "city", branch.getCity(),
                "state", branch.getState(),
                "postalCode", branch.getPostalCode(),
                "country", branch.getCountry()
        ));

        payload.put("buyer", mapOf(
                "customerId", customer.getId(),
                "customerCode", customer.getCustomerCode(),
                "legalName", firstNonBlank(customer.getLegalName(), customer.getFullName()),
                "tradeName", firstNonBlank(customer.getTradeName(), customer.getFullName()),
                "gstin", customer.getGstin(),
                "phone", firstNonBlank(customer.getContactPersonPhone(), customer.getPhone()),
                "email", firstNonBlank(customer.getContactPersonEmail(), customer.getEmail()),
                "billingAddress", customer.getBillingAddress(),
                "shippingAddress", customer.getShippingAddress(),
                "state", customer.getState(),
                "stateCode", customer.getStateCode()
        ));

        payload.put("document", mapOf(
                "invoiceNumber", invoice.getInvoiceNumber(),
                "invoiceDate", invoice.getInvoiceDate(),
                "dueDate", invoice.getDueDate(),
                "status", invoice.getStatus(),
                "placeOfSupplyStateCode", invoice.getPlaceOfSupplyStateCode(),
                "subtotal", nullSafe(invoice.getSubtotal()),
                "discountAmount", nullSafe(invoice.getDiscountAmount()),
                "taxAmount", nullSafe(invoice.getTaxAmount()),
                "totalAmount", nullSafe(invoice.getTotalAmount())
        ));

        List<Map<String, Object>> items = new ArrayList<>();
        for (SalesInvoiceLine line : lines) {
            items.add(mapOf(
                    "lineId", line.getId(),
                    "productId", line.getProductId(),
                    "hsnCode", line.getHsnSnapshot(),
                    "quantity", line.getQuantity(),
                    "baseQuantity", line.getBaseQuantity(),
                    "unitPrice", nullSafe(line.getUnitPrice()),
                    "mrp", line.getMrp(),
                    "discountAmount", nullSafe(line.getDiscountAmount()),
                    "taxableAmount", nullSafe(line.getTaxableAmount()),
                    "cgstRate", nullSafe(line.getCgstRate()),
                    "cgstAmount", nullSafe(line.getCgstAmount()),
                    "sgstRate", nullSafe(line.getSgstRate()),
                    "sgstAmount", nullSafe(line.getSgstAmount()),
                    "igstRate", nullSafe(line.getIgstRate()),
                    "igstAmount", nullSafe(line.getIgstAmount()),
                    "cessRate", nullSafe(line.getCessRate()),
                    "cessAmount", nullSafe(line.getCessAmount()),
                    "lineAmount", nullSafe(line.getLineAmount())
            ));
        }
        payload.put("items", items);

        if (DOCUMENT_TYPE_E_WAY_BILL.equals(documentType)) {
            payload.put("transport", mapOf(
                    "transporterName", request == null ? null : request.transporterName(),
                    "transporterId", request == null ? null : request.transporterId(),
                    "transportMode", request == null ? null : request.transportMode(),
                    "vehicleNumber", request == null ? null : request.vehicleNumber(),
                    "distanceKm", request == null ? null : request.distanceKm(),
                    "dispatchAddress", request == null ? null : request.dispatchAddress(),
                    "shipToAddress", request == null ? null : request.shipToAddress(),
                    "notes", request == null ? null : request.notes()
            ));
        }

        return payload;
    }

    private TaxDtos.TaxComplianceDocumentSummaryResponse toSummary(TaxComplianceDocument document) {
        return new TaxDtos.TaxComplianceDocumentSummaryResponse(
                document.getId(),
                document.getOrganizationId(),
                document.getBranchId(),
                document.getSourceType(),
                document.getSourceId(),
                document.getDocumentType(),
                document.getProviderCode(),
                document.getStatus(),
                Boolean.TRUE.equals(document.getEligibleForSubmission()),
                document.getExternalReference(),
                document.getAcknowledgementNumber(),
                document.getAcknowledgementDateTime(),
                document.getGeneratedAt(),
                document.getSubmittedAt(),
                document.getLastSyncedAt(),
                document.getErrorMessage()
        );
    }

    private TaxDtos.TaxComplianceDocumentResponse toDocumentResponse(TaxComplianceDocument document) {
        return new TaxDtos.TaxComplianceDocumentResponse(
                document.getId(),
                document.getOrganizationId(),
                document.getBranchId(),
                document.getSourceType(),
                document.getSourceId(),
                document.getDocumentType(),
                document.getProviderCode(),
                resolveComplianceProviderName(document.getProviderCode()),
                document.getStatus(),
                Boolean.TRUE.equals(document.getEligibleForSubmission()),
                readJsonList(document.getWarningJson()),
                document.getExternalReference(),
                document.getAcknowledgementNumber(),
                document.getAcknowledgementDateTime(),
                document.getGeneratedAt(),
                document.getSubmittedAt(),
                document.getLastSyncedAt(),
                document.getErrorMessage(),
                readJsonMap(document.getPayloadJson()),
                readJsonMap(document.getResponseJson())
        );
    }

    private TaxComplianceDocument requireDocument(Long documentId) {
        TaxComplianceDocument document = taxComplianceDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tax compliance document not found: " + documentId));
        accessGuard.assertOrganizationAccess(document.getOrganizationId());
        return document;
    }

    private GstComplianceProvider resolveComplianceProvider() {
        String providerCode = configuredComplianceProviderCode == null ? "DISABLED"
                : configuredComplianceProviderCode.trim().toUpperCase(Locale.ROOT);
        return complianceProviders.stream()
                .filter(provider -> provider.providerCode().equalsIgnoreCase(providerCode))
                .findFirst()
                .orElseGet(() -> complianceProviders.stream()
                        .filter(provider -> "DISABLED".equalsIgnoreCase(provider.providerCode()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Disabled GST compliance provider is missing")));
    }

    private String resolveComplianceProviderName(String providerCode) {
        return complianceProviders.stream()
                .filter(provider -> provider.providerCode().equalsIgnoreCase(providerCode))
                .map(GstComplianceProvider::providerName)
                .findFirst()
                .orElse(providerCode);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readJsonMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("Unable to deserialize GST compliance payload");
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> readJsonList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, List.class);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("Unable to deserialize GST compliance warnings");
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("Unable to serialize GST compliance payload");
        }
    }

    private static Map<String, Object> mapOf(Object... entries) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        for (int index = 0; index < entries.length; index += 2) {
            map.put((String) entries[index], entries[index + 1]);
        }
        return map;
    }

    private static BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static String firstNonBlank(String primary, String fallback) {
        return primary != null && !primary.isBlank() ? primary : fallback;
    }
}
