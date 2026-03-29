package com.retailmanagement.modules.erp.inventory.repository;

import com.retailmanagement.modules.erp.inventory.entity.InventoryBalance;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryBalanceRepository extends JpaRepository<InventoryBalance, Long> {
    List<InventoryBalance> findByOrganizationId(Long organizationId);
    List<InventoryBalance> findByOrganizationIdAndWarehouseId(Long organizationId, Long warehouseId);
    List<InventoryBalance> findByOrganizationIdAndProductId(Long organizationId, Long productId);
    List<InventoryBalance> findByOrganizationIdAndProductIdAndWarehouseId(Long organizationId, Long productId, Long warehouseId);
    java.util.Optional<InventoryBalance> findByOrganizationIdAndBranchIdAndWarehouseIdAndProductIdAndBatchId(
            Long organizationId,
            Long branchId,
            Long warehouseId,
            Long productId,
            Long batchId
    );
}
