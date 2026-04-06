package com.retailmanagement.modules.erp.catalog.service;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.erp.common.ErpSecurityUtils;
import com.retailmanagement.modules.erp.catalog.dto.ProductAttributeDtos;
import com.retailmanagement.modules.erp.catalog.entity.ProductAttributeDefinition;
import com.retailmanagement.modules.erp.catalog.entity.ProductAttributeOption;
import com.retailmanagement.modules.erp.catalog.entity.ProductAttributeScope;
import com.retailmanagement.modules.erp.catalog.entity.StoreProduct;
import com.retailmanagement.modules.erp.catalog.entity.StoreProductAttributeValue;
import com.retailmanagement.modules.erp.catalog.repository.BrandRepository;
import com.retailmanagement.modules.erp.catalog.repository.CategoryRepository;
import com.retailmanagement.modules.erp.catalog.repository.ProductAttributeDefinitionRepository;
import com.retailmanagement.modules.erp.catalog.repository.ProductAttributeOptionRepository;
import com.retailmanagement.modules.erp.catalog.repository.ProductAttributeScopeRepository;
import com.retailmanagement.modules.erp.catalog.repository.StoreProductAttributeValueRepository;
import com.retailmanagement.modules.erp.common.security.ErpAccessGuard;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductAttributeService {

    private static final List<ProductAttributeDtos.ProductAttributeTypeOptionResponse> DATA_TYPE_OPTIONS = List.of(
            new ProductAttributeDtos.ProductAttributeTypeOptionResponse("TEXT", "Text", false, false),
            new ProductAttributeDtos.ProductAttributeTypeOptionResponse("NUMBER", "Number", false, true),
            new ProductAttributeDtos.ProductAttributeTypeOptionResponse("BOOLEAN", "Boolean", false, false),
            new ProductAttributeDtos.ProductAttributeTypeOptionResponse("DATE", "Date", false, false),
            new ProductAttributeDtos.ProductAttributeTypeOptionResponse("OPTION", "Option", true, false),
            new ProductAttributeDtos.ProductAttributeTypeOptionResponse("JSON", "JSON", false, false)
    );

    private static final List<ProductAttributeDtos.ProductAttributeTypeOptionResponse> INPUT_TYPE_OPTIONS = List.of(
            new ProductAttributeDtos.ProductAttributeTypeOptionResponse("TEXT", "Text Input", false, false),
            new ProductAttributeDtos.ProductAttributeTypeOptionResponse("TEXTAREA", "Text Area", false, false),
            new ProductAttributeDtos.ProductAttributeTypeOptionResponse("NUMBER", "Number Input", false, true),
            new ProductAttributeDtos.ProductAttributeTypeOptionResponse("CHECKBOX", "Checkbox", false, false),
            new ProductAttributeDtos.ProductAttributeTypeOptionResponse("DATE", "Date Picker", false, false),
            new ProductAttributeDtos.ProductAttributeTypeOptionResponse("SELECT", "Dropdown", true, false),
            new ProductAttributeDtos.ProductAttributeTypeOptionResponse("RADIO", "Radio Group", true, false)
    );

    private final ProductAttributeDefinitionRepository definitionRepository;
    private final ProductAttributeOptionRepository optionRepository;
    private final ProductAttributeScopeRepository scopeRepository;
    private final StoreProductAttributeValueRepository valueRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ErpAccessGuard accessGuard;

    @Transactional(readOnly = true)
    public List<ProductAttributeDtos.ProductAttributeDefinitionResponse> list(Long organizationId, Long categoryId, Long brandId) {
        accessGuard.assertOrganizationAccess(organizationId);
        List<ProductAttributeDefinition> definitions = definitionRepository.findByOrganizationIdAndIsActiveTrueOrderBySortOrderAscLabelAsc(organizationId);
        if (definitions.isEmpty()) {
            return List.of();
        }
        Map<Long, List<ProductAttributeOption>> optionsByDefinitionId = optionsByDefinitionIds(definitions);
        Map<Long, List<ProductAttributeScope>> scopesByDefinitionId = scopesByDefinitionIds(organizationId, definitions);

        return definitions.stream()
                .filter(definition -> matchesScope(scopesByDefinitionId.get(definition.getId()), categoryId, brandId))
                .map(definition -> toResponse(
                        definition,
                        optionsByDefinitionId.getOrDefault(definition.getId(), List.of()),
                        scopesByDefinitionId.getOrDefault(definition.getId(), List.of())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductAttributeDtos.ProductAttributeDefinitionResponse get(Long id, Long organizationId) {
        accessGuard.assertOrganizationAccess(organizationId);
        ProductAttributeDefinition definition = definitionRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Product attribute definition not found: " + id));
        List<ProductAttributeOption> options = optionRepository.findByAttributeDefinitionIdInOrderBySortOrderAscLabelAsc(List.of(definition.getId()));
        List<ProductAttributeScope> scopes = scopeRepository.findByOrganizationIdAndAttributeDefinitionIdIn(organizationId, List.of(definition.getId()));
        return toResponse(definition, options, scopes);
    }

    @Transactional(readOnly = true)
    public ProductAttributeDtos.ProductAttributeUiConfigResponse uiConfig(Long organizationId) {
        accessGuard.assertOrganizationAccess(organizationId);
        return new ProductAttributeDtos.ProductAttributeUiConfigResponse(
                organizationId,
                ErpSecurityUtils.requirePrincipal().getAuthorities().stream()
                        .anyMatch(authority -> "catalog.manage".equals(authority.getAuthority())),
                DATA_TYPE_OPTIONS,
                INPUT_TYPE_OPTIONS
        );
    }

    public ProductAttributeDtos.ProductAttributeDefinitionResponse create(ProductAttributeDtos.UpsertProductAttributeDefinitionRequest request) {
        accessGuard.assertOrganizationAccess(request.organizationId());
        validateUpsertRequest(request);
        definitionRepository.findByOrganizationIdAndCode(request.organizationId(), normalizeCode(request.code()))
                .ifPresent(existing -> {
                    throw new BusinessException("Attribute code already exists: " + existing.getCode());
                });
        ProductAttributeDefinition definition = new ProductAttributeDefinition();
        apply(definition, request);
        definition = definitionRepository.save(definition);
        replaceOptions(definition, request.options());
        replaceScopes(definition, request.scopes());
        return get(definition.getId(), request.organizationId());
    }

    public ProductAttributeDtos.ProductAttributeDefinitionResponse update(Long id, ProductAttributeDtos.UpsertProductAttributeDefinitionRequest request) {
        accessGuard.assertOrganizationAccess(request.organizationId());
        validateUpsertRequest(request);
        ProductAttributeDefinition definition = definitionRepository.findByIdAndOrganizationId(id, request.organizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Product attribute definition not found: " + id));
        definitionRepository.findByOrganizationIdAndCode(request.organizationId(), normalizeCode(request.code()))
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BusinessException("Attribute code already exists: " + existing.getCode());
                });
        apply(definition, request);
        definition = definitionRepository.save(definition);
        replaceOptions(definition, request.options());
        replaceScopes(definition, request.scopes());
        return get(definition.getId(), request.organizationId());
    }

    public void replaceValuesForStoreProduct(StoreProduct storeProduct, List<ProductAttributeDtos.UpsertProductAttributeValueRequest> requests) {
        valueRepository.deleteByStoreProductId(storeProduct.getId());
        if (requests == null || requests.isEmpty()) {
            return;
        }

        List<ProductAttributeDefinition> definitions = new ArrayList<>();
        for (ProductAttributeDtos.UpsertProductAttributeValueRequest request : requests) {
            ProductAttributeDefinition definition = definitionRepository.findByIdAndOrganizationId(request.attributeDefinitionId(), storeProduct.getOrganizationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product attribute definition not found: " + request.attributeDefinitionId()));
            definitions.add(definition);
            validateValueRequest(definition, request, storeProduct);
        }

        List<StoreProductAttributeValue> values = new ArrayList<>();
        for (int index = 0; index < requests.size(); index++) {
            ProductAttributeDtos.UpsertProductAttributeValueRequest request = requests.get(index);
            ProductAttributeDefinition definition = definitions.get(index);
            StoreProductAttributeValue value = new StoreProductAttributeValue();
            value.setOrganizationId(storeProduct.getOrganizationId());
            value.setStoreProductId(storeProduct.getId());
            value.setAttributeDefinitionId(definition.getId());
            value.setValueText(trimToNull(request.valueText()));
            value.setValueNumber(request.valueNumber());
            value.setValueBoolean(request.valueBoolean());
            value.setValueDate(request.valueDate());
            value.setValueOptionId(request.valueOptionId());
            value.setValueJson(trimToNull(request.valueJson()));
            values.add(value);
        }
        valueRepository.saveAll(values);
    }

    @Transactional(readOnly = true)
    public Map<Long, List<ProductAttributeDtos.ProductAttributeValueResponse>> valuesByStoreProductIds(Long organizationId, List<Long> storeProductIds) {
        if (storeProductIds == null || storeProductIds.isEmpty()) {
            return Map.of();
        }
        List<StoreProductAttributeValue> values = valueRepository.findByOrganizationIdAndStoreProductIdIn(organizationId, storeProductIds);
        if (values.isEmpty()) {
            return Map.of();
        }

        List<Long> definitionIds = values.stream().map(StoreProductAttributeValue::getAttributeDefinitionId).distinct().toList();
        Map<Long, ProductAttributeDefinition> definitionsById = new HashMap<>();
        definitionRepository.findAllById(definitionIds).forEach(definition -> definitionsById.put(definition.getId(), definition));

        List<Long> optionIds = values.stream()
                .map(StoreProductAttributeValue::getValueOptionId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, ProductAttributeOption> optionsById = new HashMap<>();
        if (!optionIds.isEmpty()) {
            optionRepository.findAllById(optionIds).forEach(option -> optionsById.put(option.getId(), option));
        }

        Map<Long, List<ProductAttributeDtos.ProductAttributeValueResponse>> map = new HashMap<>();
        for (StoreProductAttributeValue value : values) {
            ProductAttributeDefinition definition = definitionsById.get(value.getAttributeDefinitionId());
            ProductAttributeOption option = value.getValueOptionId() == null ? null : optionsById.get(value.getValueOptionId());
            map.computeIfAbsent(value.getStoreProductId(), ignored -> new ArrayList<>())
                    .add(new ProductAttributeDtos.ProductAttributeValueResponse(
                            value.getId(),
                            value.getAttributeDefinitionId(),
                            definition != null ? definition.getCode() : null,
                            definition != null ? definition.getLabel() : null,
                            definition != null ? definition.getDataType() : null,
                            definition != null ? definition.getInputType() : null,
                            value.getValueText(),
                            value.getValueNumber(),
                            value.getValueBoolean(),
                            value.getValueDate(),
                            value.getValueOptionId(),
                            option != null ? option.getCode() : null,
                            option != null ? option.getLabel() : null,
                            value.getValueJson()
                    ));
        }
        return map;
    }

    private void apply(ProductAttributeDefinition definition, ProductAttributeDtos.UpsertProductAttributeDefinitionRequest request) {
        definition.setOrganizationId(request.organizationId());
        definition.setCode(normalizeCode(request.code()));
        definition.setLabel(request.label().trim());
        definition.setDescription(trimToNull(request.description()));
        definition.setDataType(normalizeUpper(request.dataType()));
        definition.setInputType(normalizeUpper(request.inputType()));
        definition.setIsRequired(Boolean.TRUE.equals(request.isRequired()));
        definition.setIsActive(request.isActive() == null || request.isActive());
        definition.setUnitLabel(trimToNull(request.unitLabel()));
        definition.setPlaceholder(trimToNull(request.placeholder()));
        definition.setHelpText(trimToNull(request.helpText()));
        definition.setSortOrder(request.sortOrder() == null ? 1 : request.sortOrder());
    }

    private void replaceOptions(ProductAttributeDefinition definition, List<ProductAttributeDtos.UpsertProductAttributeOptionRequest> requests) {
        optionRepository.deleteByAttributeDefinitionId(definition.getId());
        if (requests == null || requests.isEmpty()) {
            return;
        }
        List<ProductAttributeOption> options = new ArrayList<>();
        for (ProductAttributeDtos.UpsertProductAttributeOptionRequest request : requests) {
            ProductAttributeOption option = new ProductAttributeOption();
            option.setAttributeDefinitionId(definition.getId());
            option.setCode(normalizeCode(request.code()));
            option.setLabel(request.label().trim());
            option.setSortOrder(request.sortOrder() == null ? 1 : request.sortOrder());
            option.setIsActive(request.isActive() == null || request.isActive());
            options.add(option);
        }
        optionRepository.saveAll(options);
    }

    private void replaceScopes(ProductAttributeDefinition definition, List<ProductAttributeDtos.UpsertProductAttributeScopeRequest> requests) {
        scopeRepository.deleteByAttributeDefinitionId(definition.getId());
        if (requests == null || requests.isEmpty()) {
            return;
        }
        List<ProductAttributeScope> scopes = new ArrayList<>();
        for (ProductAttributeDtos.UpsertProductAttributeScopeRequest request : requests) {
            if (request.categoryId() != null) {
                categoryRepository.findById(request.categoryId())
                        .filter(category -> category.getOrganizationId().equals(definition.getOrganizationId()))
                        .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + request.categoryId()));
            }
            if (request.brandId() != null) {
                brandRepository.findById(request.brandId())
                        .filter(brand -> brand.getOrganizationId().equals(definition.getOrganizationId()))
                        .orElseThrow(() -> new ResourceNotFoundException("Brand not found: " + request.brandId()));
            }
            ProductAttributeScope scope = new ProductAttributeScope();
            scope.setOrganizationId(definition.getOrganizationId());
            scope.setAttributeDefinitionId(definition.getId());
            scope.setCategoryId(request.categoryId());
            scope.setBrandId(request.brandId());
            scopes.add(scope);
        }
        scopeRepository.saveAll(scopes);
    }

    private void validateUpsertRequest(ProductAttributeDtos.UpsertProductAttributeDefinitionRequest request) {
        String dataType = normalizeUpper(request.dataType());
        String inputType = normalizeUpper(request.inputType());
        List<String> allowedDataTypes = List.of("TEXT", "NUMBER", "BOOLEAN", "DATE", "OPTION", "JSON");
        List<String> allowedInputTypes = List.of("TEXT", "TEXTAREA", "NUMBER", "CHECKBOX", "DATE", "SELECT", "RADIO");
        if (!allowedDataTypes.contains(dataType)) {
            throw new BusinessException("Unsupported data type: " + request.dataType());
        }
        if (!allowedInputTypes.contains(inputType)) {
            throw new BusinessException("Unsupported input type: " + request.inputType());
        }
        boolean optionType = "OPTION".equals(dataType) || "SELECT".equals(inputType) || "RADIO".equals(inputType);
        if (optionType && (request.options() == null || request.options().isEmpty())) {
            throw new BusinessException("Options are required for selectable product attributes");
        }
    }

    private void validateValueRequest(ProductAttributeDefinition definition,
                                      ProductAttributeDtos.UpsertProductAttributeValueRequest request,
                                      StoreProduct storeProduct) {
        List<ProductAttributeScope> scopes = scopeRepository.findByOrganizationIdAndAttributeDefinitionIdIn(
                storeProduct.getOrganizationId(),
                List.of(definition.getId())
        );
        if (!matchesScope(scopes, storeProduct.getCategoryId(), storeProduct.getBrandId())) {
            throw new BusinessException("Attribute " + definition.getCode() + " is not applicable for the selected category/brand");
        }

        String dataType = definition.getDataType();
        if ("OPTION".equals(dataType)) {
            if (request.valueOptionId() == null) {
                throw new BusinessException("Attribute " + definition.getCode() + " requires an option value");
            }
            ProductAttributeOption option = optionRepository.findById(request.valueOptionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Attribute option not found: " + request.valueOptionId()));
            if (!option.getAttributeDefinitionId().equals(definition.getId())) {
                throw new BusinessException("Option does not belong to attribute " + definition.getCode());
            }
        }
        if (Boolean.TRUE.equals(definition.getIsRequired())
                && request.valueOptionId() == null
                && request.valueNumber() == null
                && request.valueBoolean() == null
                && request.valueDate() == null
                && trimToNull(request.valueText()) == null
                && trimToNull(request.valueJson()) == null) {
            throw new BusinessException("Required attribute value missing for " + definition.getCode());
        }
    }

    private Map<Long, List<ProductAttributeOption>> optionsByDefinitionIds(List<ProductAttributeDefinition> definitions) {
        List<Long> ids = definitions.stream().map(ProductAttributeDefinition::getId).toList();
        Map<Long, List<ProductAttributeOption>> map = new HashMap<>();
        optionRepository.findByAttributeDefinitionIdInOrderBySortOrderAscLabelAsc(ids)
                .forEach(option -> map.computeIfAbsent(option.getAttributeDefinitionId(), ignored -> new ArrayList<>()).add(option));
        return map;
    }

    private Map<Long, List<ProductAttributeScope>> scopesByDefinitionIds(Long organizationId, List<ProductAttributeDefinition> definitions) {
        List<Long> ids = definitions.stream().map(ProductAttributeDefinition::getId).toList();
        Map<Long, List<ProductAttributeScope>> map = new HashMap<>();
        scopeRepository.findByOrganizationIdAndAttributeDefinitionIdIn(organizationId, ids)
                .forEach(scope -> map.computeIfAbsent(scope.getAttributeDefinitionId(), ignored -> new ArrayList<>()).add(scope));
        return map;
    }

    private boolean matchesScope(List<ProductAttributeScope> scopes, Long categoryId, Long brandId) {
        if (scopes == null || scopes.isEmpty()) {
            return true;
        }
        for (ProductAttributeScope scope : scopes) {
            boolean categoryMatches = scope.getCategoryId() == null || scope.getCategoryId().equals(categoryId);
            boolean brandMatches = scope.getBrandId() == null || scope.getBrandId().equals(brandId);
            if (categoryMatches && brandMatches) {
                return true;
            }
        }
        return false;
    }

    private ProductAttributeDtos.ProductAttributeDefinitionResponse toResponse(ProductAttributeDefinition definition,
                                                                               List<ProductAttributeOption> options,
                                                                               List<ProductAttributeScope> scopes) {
        return new ProductAttributeDtos.ProductAttributeDefinitionResponse(
                definition.getId(),
                definition.getOrganizationId(),
                definition.getCode(),
                definition.getLabel(),
                definition.getDescription(),
                definition.getDataType(),
                definition.getInputType(),
                definition.getIsRequired(),
                definition.getIsActive(),
                definition.getUnitLabel(),
                definition.getPlaceholder(),
                definition.getHelpText(),
                definition.getSortOrder(),
                options.stream()
                        .map(option -> new ProductAttributeDtos.ProductAttributeOptionResponse(
                                option.getId(),
                                option.getCode(),
                                option.getLabel(),
                                option.getSortOrder(),
                                option.getIsActive()
                        ))
                        .toList(),
                scopes.stream()
                        .map(scope -> new ProductAttributeDtos.ProductAttributeScopeResponse(
                                scope.getId(),
                                scope.getCategoryId(),
                                scope.getBrandId()
                        ))
                        .toList()
        );
    }

    private String normalizeUpper(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }

    private String normalizeCode(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase();
        if (normalized.isBlank()) {
            return null;
        }
        return normalized.replace(' ', '_');
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }
}
