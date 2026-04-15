package com.retailmanagement.modules.erp.tax.service;

import org.springframework.stereotype.Component;

@Component
public class DisabledGstinLookupProvider implements GstinLookupProvider {

    @Override
    public String providerCode() {
        return "DISABLED";
    }

    @Override
    public String providerName() {
        return "Provider not configured";
    }

    @Override
    public GstinLookupResult lookup(String gstin) {
        return GstinLookupResult.unavailable(
                providerCode(),
                providerName(),
                "External GST lookup is not configured yet. UI can fall back to non-GST registration when the business is not GST-registered."
        );
    }
}
