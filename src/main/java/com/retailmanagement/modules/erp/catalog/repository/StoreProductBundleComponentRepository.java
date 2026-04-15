package com.retailmanagement.modules.erp.catalog.repository;

import com.retailmanagement.modules.erp.catalog.entity.StoreProductBundleComponent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreProductBundleComponentRepository extends JpaRepository<StoreProductBundleComponent, Long> {

    List<StoreProductBundleComponent> findByOrganizationIdAndStoreProductIdOrderBySortOrderAscIdAsc(Long organizationId, Long storeProductId);

    List<StoreProductBundleComponent> findByOrganizationIdAndStoreProductIdIn(Long organizationId, List<Long> storeProductIds);

    void deleteByOrganizationIdAndStoreProductId(Long organizationId, Long storeProductId);
}
