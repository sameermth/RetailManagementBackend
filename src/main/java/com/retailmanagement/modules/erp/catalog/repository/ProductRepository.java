package com.retailmanagement.modules.erp.catalog.repository;

import com.retailmanagement.modules.erp.catalog.entity.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository("erpMasterProductRepository")
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("""
            SELECT p
            FROM ErpMasterProduct p
            WHERE lower(trim(p.name)) = lower(trim(:name))
              AND coalesce(lower(trim(p.brandName)), '') = coalesce(lower(trim(:brandName)), '')
              AND p.baseUomId = :baseUomId
              AND p.inventoryTrackingMode = :inventoryTrackingMode
              AND p.serialTrackingEnabled = :serialTrackingEnabled
              AND p.batchTrackingEnabled = :batchTrackingEnabled
              AND p.expiryTrackingEnabled = :expiryTrackingEnabled
              AND p.fractionalQuantityAllowed = :fractionalQuantityAllowed
              AND p.isServiceItem = :serviceItem
            """)
    Optional<Product> findExactMatch(
            String name,
            String brandName,
            Long baseUomId,
            String inventoryTrackingMode,
            Boolean serialTrackingEnabled,
            Boolean batchTrackingEnabled,
            Boolean expiryTrackingEnabled,
            Boolean fractionalQuantityAllowed,
            Boolean serviceItem
    );

    List<Product> findTop20ByNameContainingIgnoreCaseOrHsnCodeContainingIgnoreCase(String name, String hsnCode);
}
