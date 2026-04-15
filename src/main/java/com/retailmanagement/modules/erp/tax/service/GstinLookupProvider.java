package com.retailmanagement.modules.erp.tax.service;

import java.time.LocalDate;

public interface GstinLookupProvider {

    String providerCode();

    String providerName();

    GstinLookupResult lookup(String gstin);

    record GstinLookupResult(
            String providerCode,
            String providerName,
            String lookupStatus,
            String legalName,
            String tradeName,
            String constitutionOfBusiness,
            String taxpayerType,
            String registrationStatus,
            Boolean active,
            LocalDate registrationDate,
            LocalDate cancellationDate,
            Address address,
            String message
    ) {
        public record Address(
                String addressLine1,
                String addressLine2,
                String location,
                String district,
                String state,
                String stateCode,
                String postalCode,
                String country
        ) {}

        public static GstinLookupResult unavailable(String providerCode, String providerName, String message) {
            return new GstinLookupResult(
                    providerCode,
                    providerName,
                    "UNAVAILABLE",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    message
            );
        }
    }
}
