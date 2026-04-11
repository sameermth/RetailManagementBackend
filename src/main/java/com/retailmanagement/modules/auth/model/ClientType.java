package com.retailmanagement.modules.auth.model;

public enum ClientType {
    WEB,
    MOBILE,
    TABLET;

    public static ClientType fromNullable(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return ClientType.valueOf(value.trim().toUpperCase());
    }
}
