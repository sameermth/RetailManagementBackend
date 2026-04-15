package com.retailmanagement.modules.erp.tax.service;

import com.retailmanagement.modules.erp.tax.entity.TaxComplianceDocument;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class SimulatedGstComplianceProvider implements GstComplianceProvider {

    @Override
    public String providerCode() {
        return "SIMULATED";
    }

    @Override
    public String providerName() {
        return "Simulated GST sandbox";
    }

    @Override
    public SubmissionResult submit(String documentType, Map<String, Object> payload, TaxComplianceDocument document) {
        LocalDateTime now = LocalDateTime.now();
        String referencePrefix = "E_INVOICE".equalsIgnoreCase(documentType) ? "SIM-EINV" : "SIM-EWB";
        String ackPrefix = "E_INVOICE".equalsIgnoreCase(documentType) ? "IRN" : "EWB";
        String externalReference = document.getExternalReference() != null
                ? document.getExternalReference()
                : referencePrefix + "-" + now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String acknowledgementNumber = ackPrefix + "-" + externalReference;

        Map<String, Object> providerResponse = new LinkedHashMap<>();
        providerResponse.put("providerStatus", "ACCEPTED");
        providerResponse.put("externalReference", externalReference);
        providerResponse.put("acknowledgementNumber", acknowledgementNumber);
        providerResponse.put("submittedAt", now);
        providerResponse.put("documentType", documentType);
        providerResponse.put("sandbox", true);
        providerResponse.put("message", "Submitted successfully to simulated GST provider");
        providerResponse.put("payloadPreview", payload);

        return new SubmissionResult(
                providerCode(),
                providerName(),
                "SUBMITTED",
                true,
                externalReference,
                acknowledgementNumber,
                now,
                providerResponse,
                null
        );
    }

    @Override
    public SyncResult syncStatus(String documentType, TaxComplianceDocument document) {
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> providerResponse = new LinkedHashMap<>();
        providerResponse.put("providerStatus", "GENERATED");
        providerResponse.put("externalReference", document.getExternalReference());
        providerResponse.put("acknowledgementNumber", document.getAcknowledgementNumber());
        providerResponse.put("lastSyncedAt", now);
        providerResponse.put("documentType", documentType);
        providerResponse.put("sandbox", true);
        providerResponse.put("message", "Status synchronized from simulated GST provider");

        return new SyncResult(
                providerCode(),
                providerName(),
                "GENERATED",
                true,
                document.getExternalReference(),
                document.getAcknowledgementNumber(),
                document.getAcknowledgementDateTime(),
                providerResponse,
                null
        );
    }
}
