package com.retailmanagement.modules.erp.inventory.service;

import com.retailmanagement.modules.erp.inventory.entity.InventoryBatch;
import com.retailmanagement.modules.erp.inventory.entity.InventoryBalance;
import com.retailmanagement.modules.erp.inventory.entity.SerialNumber;
import com.retailmanagement.modules.erp.inventory.entity.StockMovement;
import com.retailmanagement.modules.erp.inventory.repository.InventoryBatchRepository;
import com.retailmanagement.modules.erp.inventory.repository.InventoryBalanceRepository;
import com.retailmanagement.modules.erp.inventory.repository.SerialNumberRepository;
import com.retailmanagement.modules.erp.inventory.repository.StockMovementRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryQueryService {

    private final InventoryBalanceRepository inventoryBalanceRepository;
    private final StockMovementRepository stockMovementRepository;
    private final InventoryBatchRepository inventoryBatchRepository;
    private final SerialNumberRepository serialNumberRepository;

    public List<InventoryBalance> balancesByWarehouse(Long organizationId, Long warehouseId) {
        return inventoryBalanceRepository.findByOrganizationIdAndWarehouseId(organizationId, warehouseId);
    }

    public List<InventoryBalance> balancesByBin(Long organizationId, Long binLocationId) {
        return inventoryBalanceRepository.findByOrganizationIdAndBinLocationId(organizationId, binLocationId);
    }

    public List<InventoryBalance> balancesByProduct(Long organizationId, Long productId) {
        return inventoryBalanceRepository.findByOrganizationIdAndProductId(organizationId, productId);
    }

    public List<StockMovement> movementsByWarehouse(Long organizationId, Long warehouseId) {
        return stockMovementRepository.findTop100ByOrganizationIdAndWarehouseIdOrderByMovementAtDesc(organizationId, warehouseId);
    }

    public List<StockMovement> movementsByBin(Long organizationId, Long binLocationId) {
        return stockMovementRepository.findTop100ByOrganizationIdAndBinLocationIdOrderByMovementAtDesc(organizationId, binLocationId);
    }

    public List<StockMovement> movementsByProduct(Long organizationId, Long productId) {
        return stockMovementRepository.findTop100ByOrganizationIdAndProductIdOrderByMovementAtDesc(organizationId, productId);
    }

    public List<StockMovement> movementsByReference(Long organizationId, String referenceType, Long referenceId) {
        return stockMovementRepository.findTop100ByOrganizationIdAndReferenceTypeAndReferenceIdOrderByMovementAtDesc(
                organizationId,
                referenceType,
                referenceId
        );
    }

    public List<InventoryBatch> batchesByProduct(Long organizationId, Long productId) {
        return inventoryBatchRepository.findByOrganizationIdAndProductId(organizationId, productId);
    }

    public List<SerialNumber> serialsByProduct(Long organizationId, Long productId) {
        return serialNumberRepository.findByOrganizationIdAndProductId(organizationId, productId);
    }
}
