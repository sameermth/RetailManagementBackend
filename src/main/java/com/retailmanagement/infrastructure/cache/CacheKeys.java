package com.retailmanagement.infrastructure.cache;

public class CacheKeys {

    // Product cache keys
    public static final String PRODUCT_ALL = "products:all";
    public static final String PRODUCT_BY_ID = "products:id:";
    public static final String PRODUCT_BY_SKU = "products:sku:";
    public static final String PRODUCT_BY_CATEGORY = "products:category:";
    public static final String PRODUCT_BY_BRAND = "products:brand:";

    // Category cache keys
    public static final String CATEGORY_ALL = "categories:all";
    public static final String CATEGORY_BY_ID = "categories:id:";
    public static final String CATEGORY_ROOT = "categories:root";

    // Customer cache keys
    public static final String CUSTOMER_ALL = "customers:all";
    public static final String CUSTOMER_BY_ID = "customers:id:";
    public static final String CUSTOMER_BY_CODE = "customers:code:";

    // User cache keys
    public static final String USER_BY_ID = "users:id:";
    public static final String USER_BY_USERNAME = "users:username:";

    // Dashboard cache keys
    public static final String DASHBOARD_SUMMARY = "dashboard:summary";
    public static final String DASHBOARD_SALES_TODAY = "dashboard:sales:today";
    public static final String DASHBOARD_TOP_PRODUCTS = "dashboard:products:top";

    // Report cache keys
    public static final String REPORT_SALES_SUMMARY = "report:sales:summary:";
    public static final String REPORT_INVENTORY_SUMMARY = "report:inventory:summary:";

    // Cache durations (in seconds)
    public static final int DURATION_MINUTE = 60;
    public static final int DURATION_HOUR = 3600;
    public static final int DURATION_DAY = 86400;
    public static final int DURATION_WEEK = 604800;
}