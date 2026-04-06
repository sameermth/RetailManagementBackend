package com.retailmanagement.modules.erp.catalog.repository;

import com.retailmanagement.modules.erp.catalog.entity.ProductAttributeScope;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductAttributeScopeRepository extends JpaRepository<ProductAttributeScope, Long> {

    List<ProductAttributeScope> findByOrganizationIdAndAttributeDefinitionIdIn(Long organizationId, List<Long> attributeDefinitionIds);

    void deleteByAttributeDefinitionId(Long attributeDefinitionId);
}
