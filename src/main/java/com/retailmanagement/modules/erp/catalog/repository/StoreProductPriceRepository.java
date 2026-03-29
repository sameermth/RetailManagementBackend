package com.retailmanagement.modules.erp.catalog.repository;

import com.retailmanagement.modules.erp.catalog.entity.StoreProductPrice;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository("erpStoreProductPriceRepository")
public interface StoreProductPriceRepository extends JpaRepository<StoreProductPrice, Long> {
    List<StoreProductPrice> findByOrganizationIdAndStoreProductIdOrderByEffectiveFromDescIdDesc(Long organizationId, Long storeProductId);
    Optional<StoreProductPrice> findByIdAndOrganizationId(Long id, Long organizationId);
}
