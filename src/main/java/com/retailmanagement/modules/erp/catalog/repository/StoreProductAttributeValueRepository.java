package com.retailmanagement.modules.erp.catalog.repository;

import com.retailmanagement.modules.erp.catalog.entity.StoreProductAttributeValue;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreProductAttributeValueRepository extends JpaRepository<StoreProductAttributeValue, Long> {

    List<StoreProductAttributeValue> findByOrganizationIdAndStoreProductIdIn(Long organizationId, List<Long> storeProductIds);

    void deleteByStoreProductId(Long storeProductId);
}
