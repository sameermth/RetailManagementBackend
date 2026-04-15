package com.retailmanagement.modules.erp.tax.service;

import com.retailmanagement.modules.erp.tax.dto.TaxDtos;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GstinLookupService {

    private static final Pattern GSTIN_PATTERN = Pattern.compile("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$");

    private final List<GstinLookupProvider> providers;

    @Value("${erp.tax.integration.lookup-provider:DISABLED}")
    private String configuredProviderCode;

    public TaxDtos.GstinLookupResponse lookup(String gstin) {
        String normalized = normalizeGstin(gstin);
        if (!isValidGstinFormat(normalized)) {
            return new TaxDtos.GstinLookupResponse(
                    normalized,
                    false,
                    true,
                    null,
                    null,
                    "INVALID",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "GSTIN format is invalid. UI may continue with non-GST business flow if appropriate.",
                    LocalDateTime.now()
            );
        }

        GstinLookupProvider provider = resolveProvider();
        GstinLookupProvider.GstinLookupResult result = provider.lookup(normalized);
        TaxDtos.GstinLookupAddressResponse address = result.address() == null ? null : new TaxDtos.GstinLookupAddressResponse(
                result.address().addressLine1(),
                result.address().addressLine2(),
                result.address().location(),
                result.address().district(),
                result.address().state(),
                result.address().stateCode(),
                result.address().postalCode(),
                result.address().country()
        );

        return new TaxDtos.GstinLookupResponse(
                normalized,
                true,
                true,
                result.providerCode(),
                result.providerName(),
                result.lookupStatus(),
                result.legalName(),
                result.tradeName(),
                result.constitutionOfBusiness(),
                result.taxpayerType(),
                result.registrationStatus(),
                result.active(),
                result.registrationDate(),
                result.cancellationDate(),
                address,
                result.message(),
                LocalDateTime.now()
        );
    }

    public String providerCode() {
        return resolveProvider().providerCode();
    }

    public String providerName() {
        return resolveProvider().providerName();
    }

    static boolean isValidGstinFormat(String gstin) {
        return gstin != null && GSTIN_PATTERN.matcher(gstin).matches();
    }

    static String normalizeGstin(String gstin) {
        return gstin == null ? null : gstin.trim().toUpperCase(Locale.ROOT);
    }

    private GstinLookupProvider resolveProvider() {
        String providerCode = configuredProviderCode == null ? "DISABLED" : configuredProviderCode.trim().toUpperCase(Locale.ROOT);
        return providers.stream()
                .filter(provider -> provider.providerCode().equalsIgnoreCase(providerCode))
                .findFirst()
                .orElseGet(() -> providers.stream()
                        .filter(provider -> "DISABLED".equalsIgnoreCase(provider.providerCode()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Disabled GST lookup provider is missing")));
    }
}
