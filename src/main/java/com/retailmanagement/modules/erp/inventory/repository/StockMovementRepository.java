package com.retailmanagement.modules.erp.inventory.repository;

import com.retailmanagement.modules.erp.inventory.entity.StockMovement;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository("erpStockMovementRepository")
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    List<StockMovement> findTop100ByOrganizationIdAndWarehouseIdOrderByMovementAtDesc(Long organizationId, Long warehouseId);
    List<StockMovement> findTop100ByOrganizationIdAndBinLocationIdOrderByMovementAtDesc(Long organizationId, Long binLocationId);
    List<StockMovement> findTop100ByOrganizationIdAndProductIdOrderByMovementAtDesc(Long organizationId, Long productId);
    List<StockMovement> findTop100ByOrganizationIdAndReferenceTypeAndReferenceIdOrderByMovementAtDesc(
            Long organizationId,
            String referenceType,
            Long referenceId
    );
}
