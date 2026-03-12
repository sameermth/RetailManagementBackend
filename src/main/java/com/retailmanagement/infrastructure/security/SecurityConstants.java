package com.retailmanagement.infrastructure.security;

public class SecurityConstants {

    // JWT Constants
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String HEADER_STRING = "Authorization";
    public static final String SIGN_UP_URL = "/api/auth/register";
    public static final String LOGIN_URL = "/api/auth/login";

    // Role Constants
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_MANAGER = "MANAGER";
    public static final String ROLE_CASHIER = "CASHIER";
    public static final String ROLE_INVENTORY_MANAGER = "INVENTORY_MANAGER";
    public static final String ROLE_PURCHASE_MANAGER = "PURCHASE_MANAGER";
    public static final String ROLE_SALES_MANAGER = "SALES_MANAGER";
    public static final String ROLE_ACCOUNTANT = "ACCOUNTANT";
    public static final String ROLE_EMPLOYEE = "EMPLOYEE";

    // Permission Constants
    public static final String PERMISSION_PRODUCT_READ = "PRODUCT_READ";
    public static final String PERMISSION_PRODUCT_WRITE = "PRODUCT_WRITE";
    public static final String PERMISSION_PRODUCT_DELETE = "PRODUCT_DELETE";

    public static final String PERMISSION_INVENTORY_READ = "INVENTORY_READ";
    public static final String PERMISSION_INVENTORY_WRITE = "INVENTORY_WRITE";
    public static final String PERMISSION_INVENTORY_ADJUST = "INVENTORY_ADJUST";

    public static final String PERMISSION_SALES_READ = "SALES_READ";
    public static final String PERMISSION_SALES_WRITE = "SALES_WRITE";
    public static final String PERMISSION_SALES_DELETE = "SALES_DELETE";
    public static final String PERMISSION_SALES_REFUND = "SALES_REFUND";

    public static final String PERMISSION_PURCHASE_READ = "PURCHASE_READ";
    public static final String PERMISSION_PURCHASE_WRITE = "PURCHASE_WRITE";
    public static final String PERMISSION_PURCHASE_APPROVE = "PURCHASE_APPROVE";

    public static final String PERMISSION_CUSTOMER_READ = "CUSTOMER_READ";
    public static final String PERMISSION_CUSTOMER_WRITE = "CUSTOMER_WRITE";
    public static final String PERMISSION_CUSTOMER_DUE = "CUSTOMER_DUE";

    public static final String PERMISSION_SUPPLIER_READ = "SUPPLIER_READ";
    public static final String PERMISSION_SUPPLIER_WRITE = "SUPPLIER_WRITE";

    public static final String PERMISSION_DISTRIBUTOR_READ = "DISTRIBUTOR_READ";
    public static final String PERMISSION_DISTRIBUTOR_WRITE = "DISTRIBUTOR_WRITE";

    public static final String PERMISSION_EXPENSE_READ = "EXPENSE_READ";
    public static final String PERMISSION_EXPENSE_WRITE = "EXPENSE_WRITE";
    public static final String PERMISSION_EXPENSE_APPROVE = "EXPENSE_APPROVE";

    public static final String PERMISSION_REPORT_READ = "REPORT_READ";
    public static final String PERMISSION_REPORT_GENERATE = "REPORT_GENERATE";

    public static final String PERMISSION_USER_READ = "USER_READ";
    public static final String PERMISSION_USER_WRITE = "USER_WRITE";
    public static final String PERMISSION_USER_DELETE = "USER_DELETE";

    // API Paths
    public static final String[] PUBLIC_URLS = {
            "/api/auth/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/webjars/**",
            "/actuator/health"
    };

    // Cache Keys
    public static final String CACHE_PRODUCTS = "products";
    public static final String CACHE_CATEGORIES = "categories";
    public static final String CACHE_CUSTOMERS = "customers";
    public static final String CACHE_USERS = "users";

    // Default Values
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
}