package com.retailmanagement.modules.erp.inventory.repository;

import com.retailmanagement.modules.erp.inventory.entity.InventoryBatch;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryBatchRepository extends JpaRepository<InventoryBatch, Long> {
    List<InventoryBatch> findByOrganizationIdAndProductId(Long organizationId, Long productId);
    Optional<InventoryBatch> findByOrganizationIdAndProductIdAndBatchNumber(Long organizationId, Long productId, String batchNumber);
    Optional<InventoryBatch> findFirstByOrganizationIdAndBatchNumberIgnoreCase(Long organizationId, String batchNumber);
    Optional<InventoryBatch> findFirstByOrganizationIdAndManufacturerBatchNumberIgnoreCase(Long organizationId, String manufacturerBatchNumber);
}
