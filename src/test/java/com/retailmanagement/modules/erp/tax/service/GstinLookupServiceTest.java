package com.retailmanagement.modules.erp.tax.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class GstinLookupServiceTest {

    @Test
    void lookup_returnsInvalidForBadFormat() {
        GstinLookupService service = new GstinLookupService(List.of(new DisabledGstinLookupProvider()));

        var response = service.lookup("123");

        assertFalse(response.validFormat());
        assertEquals("INVALID", response.lookupStatus());
        assertTrue(response.eligibleForNonGstFallback());
    }

    @Test
    void lookup_returnsUnavailableWhenProviderDisabled() {
        GstinLookupService service = new GstinLookupService(List.of(new DisabledGstinLookupProvider()));
        injectProviderCode(service, "DISABLED");

        var response = service.lookup("24ABCDE1234F1Z5");

        assertTrue(response.validFormat());
        assertEquals("DISABLED", response.providerCode());
        assertEquals("UNAVAILABLE", response.lookupStatus());
    }

    private static void injectProviderCode(GstinLookupService service, String value) {
        try {
            var field = GstinLookupService.class.getDeclaredField("configuredProviderCode");
            field.setAccessible(true);
            field.set(service, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
