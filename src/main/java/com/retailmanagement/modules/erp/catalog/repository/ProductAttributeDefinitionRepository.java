package com.retailmanagement.modules.erp.catalog.repository;

import com.retailmanagement.modules.erp.catalog.entity.ProductAttributeDefinition;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductAttributeDefinitionRepository extends JpaRepository<ProductAttributeDefinition, Long> {

    List<ProductAttributeDefinition> findByOrganizationIdAndIsActiveTrueOrderBySortOrderAscLabelAsc(Long organizationId);

    Optional<ProductAttributeDefinition> findByIdAndOrganizationId(Long id, Long organizationId);

    Optional<ProductAttributeDefinition> findByOrganizationIdAndCode(Long organizationId, String code);
}
