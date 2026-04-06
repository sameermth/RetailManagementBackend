package com.retailmanagement.modules.erp.catalog.repository;

import com.retailmanagement.modules.erp.catalog.entity.ProductAttributeOption;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductAttributeOptionRepository extends JpaRepository<ProductAttributeOption, Long> {

    List<ProductAttributeOption> findByAttributeDefinitionIdInOrderBySortOrderAscLabelAsc(List<Long> attributeDefinitionIds);

    void deleteByAttributeDefinitionId(Long attributeDefinitionId);
}
