package com.retailmanagement.modules.erp.catalog.repository;

import com.retailmanagement.modules.erp.catalog.entity.StoreProduct;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository("erpStoreProductRepository")
public interface StoreProductRepository extends JpaRepository<StoreProduct, Long> {
    List<StoreProduct> findByOrganizationId(Long organizationId);
    Optional<StoreProduct> findByOrganizationIdAndSku(Long organizationId, String sku);
    Optional<StoreProduct> findFirstByOrganizationIdAndSkuIgnoreCase(Long organizationId, String sku);
    Optional<StoreProduct> findByOrganizationIdAndProductId(Long organizationId, Long productId);
}
