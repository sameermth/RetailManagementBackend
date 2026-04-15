package com.retailmanagement.modules.erp.tax.service;

import com.retailmanagement.modules.erp.tax.entity.TaxComplianceDocument;
import java.time.LocalDateTime;
import java.util.Map;

public interface GstComplianceProvider {

    String providerCode();

    String providerName();

    SubmissionResult submit(String documentType, Map<String, Object> payload, TaxComplianceDocument document);

    SyncResult syncStatus(String documentType, TaxComplianceDocument document);

    record SubmissionResult(
            String providerCode,
            String providerName,
            String documentStatus,
            boolean accepted,
            String externalReference,
            String acknowledgementNumber,
            LocalDateTime acknowledgementDateTime,
            Map<String, Object> responsePayload,
            String errorMessage
    ) {}

    record SyncResult(
            String providerCode,
            String providerName,
            String documentStatus,
            boolean terminal,
            String externalReference,
            String acknowledgementNumber,
            LocalDateTime acknowledgementDateTime,
            Map<String, Object> responsePayload,
            String errorMessage
    ) {}
}
