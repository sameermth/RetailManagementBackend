package com.retailmanagement.modules.erp.tax.service;

import com.retailmanagement.modules.erp.tax.entity.TaxComplianceDocument;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class DisabledGstComplianceProvider implements GstComplianceProvider {

    @Override
    public String providerCode() {
        return "DISABLED";
    }

    @Override
    public String providerName() {
        return "Provider not configured";
    }

    @Override
    public SubmissionResult submit(String documentType, Map<String, Object> payload, TaxComplianceDocument document) {
        return new SubmissionResult(
                providerCode(),
                providerName(),
                "PROVIDER_UNAVAILABLE",
                false,
                document.getExternalReference(),
                document.getAcknowledgementNumber(),
                document.getAcknowledgementDateTime(),
                Map.of(
                        "documentType", documentType,
                        "action", "SUBMIT",
                        "providerStatus", "UNAVAILABLE",
                        "message", "GST compliance provider is not configured yet."
                ),
                "GST compliance provider is not configured yet."
        );
    }

    @Override
    public SyncResult syncStatus(String documentType, TaxComplianceDocument document) {
        return new SyncResult(
                providerCode(),
                providerName(),
                document.getStatus(),
                false,
                document.getExternalReference(),
                document.getAcknowledgementNumber(),
                document.getAcknowledgementDateTime(),
                Map.of(
                        "documentType", documentType,
                        "action", "SYNC_STATUS",
                        "providerStatus", "UNAVAILABLE",
                        "message", "GST compliance provider is not configured yet."
                ),
                "GST compliance provider is not configured yet."
        );
    }
}
