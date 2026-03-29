package com.retailmanagement.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springdoc.core.customizers.OpenApiCustomizer;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("Retail Management API")
                        .version("1.0")
                        .description("Retail Management System API Documentation")
                        .license(new License().name("Apache 2.0")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }

    @Bean
    public OpenApiCustomizer erpOpenApiCustomizer() {
        return openApi -> {
            Map<String, String> tagMap = new LinkedHashMap<>();
            tagMap.put("product-controller", "ERP Catalog");
            tagMap.put("organization-controller", "ERP Organizations");
            tagMap.put("erp-sales-controller", "ERP Sales");
            tagMap.put("erp-purchase-controller", "ERP Purchases");
            tagMap.put("erp-finance-controller", "ERP Finance");
            tagMap.put("erp-expense-controller", "ERP Expenses");
            tagMap.put("subscription-controller", "ERP Subscriptions");
            tagMap.put("erp-approval-controller", "ERP Approvals");
            tagMap.put("erp-service-warranty-controller", "ERP Service");
            tagMap.put("inventory-balance-controller", "ERP Inventory Balances");
            tagMap.put("inventory-operations-controller", "ERP Inventory Operations");
            tagMap.put("inventory-tracking-controller", "ERP Inventory Tracking");
            tagMap.put("stock-movement-controller", "ERP Stock Movements");
            tagMap.put("audit-event-controller", "ERP Audit");
            tagMap.put("tax-registration-controller", "ERP Tax");
            tagMap.put("erp-system-controller", "ERP System");

            Map<String, String> descriptions = new LinkedHashMap<>();
            descriptions.put("ERP Catalog", "ERP product and store product endpoints");
            descriptions.put("ERP Organizations", "ERP organization management endpoints");
            descriptions.put("ERP Sales", "ERP sales invoices and customer receipt endpoints");
            descriptions.put("ERP Purchases", "ERP purchase orders, receipts, and supplier payment endpoints");
            descriptions.put("ERP Finance", "ERP accounts, vouchers, and ledger reporting endpoints");
            descriptions.put("ERP Expenses", "ERP expense category and expense posting endpoints");
            descriptions.put("ERP Subscriptions", "ERP subscription and plan activation endpoints");
            descriptions.put("ERP Approvals", "ERP approval rules and approval request endpoints");
            descriptions.put("ERP Service", "ERP service ticket and warranty claim endpoints");
            descriptions.put("ERP Inventory Balances", "ERP inventory balance query endpoints");
            descriptions.put("ERP Inventory Operations", "ERP manual adjustment and stock transfer endpoints");
            descriptions.put("ERP Inventory Tracking", "ERP batch and serial lookup endpoints");
            descriptions.put("ERP Stock Movements", "ERP stock movement query endpoints");
            descriptions.put("ERP Audit", "ERP audit event query endpoints");
            descriptions.put("ERP Tax", "ERP tax registration and GST threshold endpoints");
            descriptions.put("ERP System", "ERP system status endpoints");

            openApi.getPaths().forEach((path, pathItem) -> applyTagAndSummary(path, pathItem, tagMap));

            List<Tag> tags = openApi.getTags() == null ? new ArrayList<>() : new ArrayList<>(openApi.getTags());
            descriptions.forEach((name, description) -> {
                boolean exists = tags.stream().anyMatch(tag -> name.equals(tag.getName()));
                if (!exists) {
                    tags.add(new Tag().name(name).description(description));
                }
            });
            openApi.setTags(tags);
        };
    }

    private void applyTagAndSummary(String path, PathItem pathItem, Map<String, String> tagMap) {
        if (pathItem == null || pathItem.readOperationsMap() == null) {
            return;
        }
        pathItem.readOperationsMap().forEach((httpMethod, operation) -> {
            rewriteTags(operation, tagMap);
            if (operation.getSummary() == null || operation.getSummary().isBlank()) {
                operation.setSummary(defaultSummary(httpMethod.name(), path));
            }
        });
    }

    private void rewriteTags(Operation operation, Map<String, String> tagMap) {
        if (operation.getTags() == null || operation.getTags().isEmpty()) {
            return;
        }
        List<String> rewritten = operation.getTags().stream()
                .map(tag -> tagMap.getOrDefault(tag, tag))
                .distinct()
                .toList();
        operation.setTags(rewritten);
    }

    private String defaultSummary(String httpMethod, String path) {
        String action = switch (httpMethod) {
            case "GET" -> "Get";
            case "POST" -> "Create";
            case "PUT" -> "Update";
            case "DELETE" -> "Delete";
            default -> "Handle";
        };
        return action + " " + path.substring(path.lastIndexOf('/') + 1).replace('-', ' ');
    }
}
