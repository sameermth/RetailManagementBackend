package com.retailmanagement.modules.erp.tax.service;

import com.retailmanagement.modules.erp.tax.entity.TaxComplianceDocument;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class HttpGstComplianceProvider implements GstComplianceProvider {

    private final RestTemplate restTemplate;

    @Value("${erp.tax.integration.http.enabled:false}")
    private boolean enabled;

    @Value("${erp.tax.integration.http.provider-code:HTTP}")
    private String providerCode;

    @Value("${erp.tax.integration.http.provider-name:Configured GST provider}")
    private String providerName;

    @Value("${erp.tax.integration.http.base-url:}")
    private String baseUrl;

    @Value("${erp.tax.integration.http.submit-path:/submit}")
    private String submitPath;

    @Value("${erp.tax.integration.http.sync-path:/sync-status}")
    private String syncPath;

    @Value("${erp.tax.integration.http.auth-header-name:}")
    private String authHeaderName;

    @Value("${erp.tax.integration.http.auth-token:}")
    private String authToken;

    @Override
    public String providerCode() {
        return providerCode;
    }

    @Override
    public String providerName() {
        return providerName;
    }

    @Override
    public SubmissionResult submit(String documentType, Map<String, Object> payload, TaxComplianceDocument document) {
        if (!isConfigured()) {
            return unavailableSubmission(documentType, document, "HTTP GST compliance provider is not configured");
        }
        try {
            Map<String, Object> response = post(submitPath, Map.of(
                    "action", "SUBMIT",
                    "documentType", documentType,
                    "documentId", document.getId(),
                    "sourceId", document.getSourceId(),
                    "externalReference", document.getExternalReference(),
                    "payload", payload
            ));
            return new SubmissionResult(
                    providerCode(),
                    providerName(),
                    stringValue(response, "documentStatus", "SUBMITTED"),
                    booleanValue(response, "accepted", true),
                    stringValue(response, "externalReference", document.getExternalReference()),
                    stringValue(response, "acknowledgementNumber", document.getAcknowledgementNumber()),
                    dateTimeValue(response.get("acknowledgementDateTime")),
                    response,
                    stringValue(response, "errorMessage", null)
            );
        } catch (Exception exception) {
            return unavailableSubmission(documentType, document, exception.getMessage());
        }
    }

    @Override
    public SyncResult syncStatus(String documentType, TaxComplianceDocument document) {
        if (!isConfigured()) {
            return unavailableSync(documentType, document, "HTTP GST compliance provider is not configured");
        }
        try {
            Map<String, Object> response = post(syncPath, Map.of(
                    "action", "SYNC_STATUS",
                    "documentType", documentType,
                    "documentId", document.getId(),
                    "sourceId", document.getSourceId(),
                    "externalReference", document.getExternalReference(),
                    "acknowledgementNumber", document.getAcknowledgementNumber()
            ));
            String status = stringValue(response, "documentStatus", document.getStatus());
            return new SyncResult(
                    providerCode(),
                    providerName(),
                    status,
                    booleanValue(response, "terminal", "GENERATED".equalsIgnoreCase(status)),
                    stringValue(response, "externalReference", document.getExternalReference()),
                    stringValue(response, "acknowledgementNumber", document.getAcknowledgementNumber()),
                    dateTimeValue(response.get("acknowledgementDateTime")),
                    response,
                    stringValue(response, "errorMessage", null)
            );
        } catch (Exception exception) {
            return unavailableSync(documentType, document, exception.getMessage());
        }
    }

    private boolean isConfigured() {
        return enabled && baseUrl != null && !baseUrl.isBlank();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> post(String path, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (authHeaderName != null && !authHeaderName.isBlank() && authToken != null && !authToken.isBlank()) {
            headers.set(authHeaderName, authToken);
        }
        Object response = restTemplate.postForObject(baseUrl + path, new HttpEntity<>(body, headers), Object.class);
        if (response instanceof Map<?, ?> mapResponse) {
            Map<String, Object> casted = new LinkedHashMap<>();
            mapResponse.forEach((key, value) -> casted.put(String.valueOf(key), value));
            return casted;
        }
        return Map.of("rawResponse", response);
    }

    private SubmissionResult unavailableSubmission(String documentType, TaxComplianceDocument document, String message) {
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
                        "message", message
                ),
                message
        );
    }

    private SyncResult unavailableSync(String documentType, TaxComplianceDocument document, String message) {
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
                        "message", message
                ),
                message
        );
    }

    private String stringValue(Map<String, Object> response, String key, String fallback) {
        Object value = response.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    private boolean booleanValue(Map<String, Object> response, String key, boolean fallback) {
        Object value = response.get(key);
        if (value == null) {
            return fallback;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private LocalDateTime dateTimeValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        try {
            return LocalDateTime.parse(String.valueOf(value));
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }
}
