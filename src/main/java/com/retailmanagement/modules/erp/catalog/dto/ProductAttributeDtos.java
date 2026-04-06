package com.retailmanagement.modules.erp.catalog.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public final class ProductAttributeDtos {
    private ProductAttributeDtos() {}

    public record ProductAttributeOptionResponse(
            Long id,
            String code,
            String label,
            Integer sortOrder,
            Boolean isActive
    ) {}

    public record ProductAttributeScopeResponse(
            Long id,
            Long categoryId,
            Long brandId
    ) {}

    public record UpsertProductAttributeValueRequest(
            @NotNull Long attributeDefinitionId,
            String valueText,
            java.math.BigDecimal valueNumber,
            Boolean valueBoolean,
            java.time.LocalDate valueDate,
            Long valueOptionId,
            String valueJson
    ) {}

    public record ProductAttributeValueResponse(
            Long id,
            Long attributeDefinitionId,
            String attributeCode,
            String attributeLabel,
            String dataType,
            String inputType,
            String valueText,
            java.math.BigDecimal valueNumber,
            Boolean valueBoolean,
            java.time.LocalDate valueDate,
            Long valueOptionId,
            String valueOptionCode,
            String valueOptionLabel,
            String valueJson
    ) {}

    public record ProductAttributeDefinitionResponse(
            Long id,
            Long organizationId,
            String code,
            String label,
            String description,
            String dataType,
            String inputType,
            Boolean isRequired,
            Boolean isActive,
            String unitLabel,
            String placeholder,
            String helpText,
            Integer sortOrder,
            List<ProductAttributeOptionResponse> options,
            List<ProductAttributeScopeResponse> scopes
    ) {}

    public record ProductAttributeTypeOptionResponse(
            String code,
            String label,
            Boolean supportsOptions,
            Boolean supportsUnitLabel
    ) {}

    public record ProductAttributeUiConfigResponse(
            Long organizationId,
            Boolean canManage,
            List<ProductAttributeTypeOptionResponse> dataTypes,
            List<ProductAttributeTypeOptionResponse> inputTypes
    ) {}

    public record UpsertProductAttributeOptionRequest(
            @NotBlank String code,
            @NotBlank String label,
            Integer sortOrder,
            Boolean isActive
    ) {}

    public record UpsertProductAttributeScopeRequest(
            Long categoryId,
            Long brandId
    ) {}

    public record UpsertProductAttributeDefinitionRequest(
            @NotNull Long organizationId,
            @NotBlank String code,
            @NotBlank String label,
            String description,
            @NotBlank String dataType,
            @NotBlank String inputType,
            Boolean isRequired,
            Boolean isActive,
            String unitLabel,
            String placeholder,
            String helpText,
            Integer sortOrder,
            @Valid List<UpsertProductAttributeOptionRequest> options,
            @Valid List<UpsertProductAttributeScopeRequest> scopes
    ) {}
}
