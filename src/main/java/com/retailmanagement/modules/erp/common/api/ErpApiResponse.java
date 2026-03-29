package com.retailmanagement.modules.erp.common.api;

import java.time.OffsetDateTime;

public record ErpApiResponse<T>(boolean success, T data, String message, OffsetDateTime timestamp) {
    public static <T> ErpApiResponse<T> ok(T data) { return new ErpApiResponse<>(true, data, null, OffsetDateTime.now()); }
    public static <T> ErpApiResponse<T> ok(T data, String message) { return new ErpApiResponse<>(true, data, message, OffsetDateTime.now()); }
}
