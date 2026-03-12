package com.retailmanagement.modules.notification.enums;

public enum NotificationType {
    // System notifications
    SYSTEM_ALERT,
    SYSTEM_MAINTENANCE,

    // Sales notifications
    SALE_CREATED,
    SALE_CONFIRMED,
    SALE_CANCELLED,
    SALE_REFUNDED,
    INVOICE_GENERATED,
    PAYMENT_RECEIVED,

    // Purchase notifications
    PURCHASE_ORDER_CREATED,
    PURCHASE_ORDER_APPROVED,
    PURCHASE_ORDER_RECEIVED,
    PURCHASE_ORDER_CANCELLED,
    SUPPLIER_PAYMENT_MADE,

    // Inventory notifications
    LOW_STOCK_ALERT,
    OUT_OF_STOCK_ALERT,
    STOCK_TRANSFER,
    EXPIRY_ALERT,

    // Customer notifications
    CUSTOMER_DUE_REMINDER,
    CUSTOMER_PAYMENT_CONFIRMATION,
    CUSTOMER_WELCOME,
    CUSTOMER_BIRTHDAY,

    // Distributor notifications
    DISTRIBUTOR_ORDER_CREATED,
    DISTRIBUTOR_ORDER_SHIPPED,
    DISTRIBUTOR_ORDER_DELIVERED,
    DISTRIBUTOR_PAYMENT_RECEIVED,

    // Due notifications
    DUE_CREATED,
    DUE_REMINDER,
    DUE_OVERDUE,
    DUE_PAID,

    // Expense notifications
    EXPENSE_CREATED,
    EXPENSE_APPROVED,
    EXPENSE_REJECTED,
    EXPENSE_PAID,

    // Report notifications
    REPORT_GENERATED,
    REPORT_SCHEDULED,
    REPORT_DELIVERED,

    // User notifications
    USER_CREATED,
    USER_UPDATED,
    PASSWORD_CHANGED,
    LOGIN_ALERT,

    // Marketing notifications
    PROMOTIONAL_OFFER,
    NEW_PRODUCT_ARRIVAL,
    SEASONAL_GREETINGS
}