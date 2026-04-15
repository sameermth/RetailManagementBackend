package com.retailmanagement.modules.erp.inventory.service;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.modules.erp.audit.service.AuditEventWriter;
import com.retailmanagement.modules.erp.inventory.entity.InventoryBalance;
import com.retailmanagement.modules.erp.inventory.entity.StockMovement;
import com.retailmanagement.modules.erp.inventory.repository.InventoryBalanceRepository;
import com.retailmanagement.modules.erp.inventory.repository.StockMovementRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class InventoryPostingService {

    private final InventoryBalanceRepository inventoryBalanceRepository;
    private final StockMovementRepository stockMovementRepository;
    private final AuditEventWriter auditEventWriter;

    public StockMovement postMovement(
            Long organizationId,
            Long branchId,
            Long warehouseId,
            Long binLocationId,
            Long productId,
            Long batchId,
            Long uomId,
            BigDecimal quantity,
            BigDecimal baseQuantity,
            String direction,
            String movementType,
            String referenceType,
            Long referenceId,
            String referenceNumber,
            BigDecimal unitCost,
            String summaryPayload
    ) {
        InventoryBalance balance = inventoryBalanceRepository
                .findByOrganizationIdAndBranchIdAndWarehouseIdAndProductIdAndBatchIdAndBinLocationId(
                        organizationId, branchId, warehouseId, productId, batchId, binLocationId
                )
                .orElseGet(() -> {
                    InventoryBalance ib = new InventoryBalance();
                    ib.setOrganizationId(organizationId);
                    ib.setBranchId(branchId);
                    ib.setWarehouseId(warehouseId);
                    ib.setBinLocationId(binLocationId);
                    ib.setProductId(productId);
                    ib.setBatchId(batchId);
                    ib.setOnHandBaseQuantity(BigDecimal.ZERO);
                    ib.setReservedBaseQuantity(BigDecimal.ZERO);
                    ib.setAvailableBaseQuantity(BigDecimal.ZERO);
                    ib.setAvgCost(unitCost);
                    return ib;
                });

        BigDecimal signedDelta = "OUT".equalsIgnoreCase(direction)
                ? baseQuantity.negate()
                : baseQuantity;

        BigDecimal nextOnHand = balance.getOnHandBaseQuantity().add(signedDelta);
        if (nextOnHand.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Insufficient stock for product " + productId + " in warehouse " + warehouseId);
        }

        balance.setOnHandBaseQuantity(nextOnHand);
        balance.setAvailableBaseQuantity(nextOnHand.subtract(
                balance.getReservedBaseQuantity() == null ? BigDecimal.ZERO : balance.getReservedBaseQuantity()
        ));
        if (unitCost != null) {
            balance.setAvgCost(unitCost);
        }
        inventoryBalanceRepository.save(balance);

        StockMovement movement = new StockMovement();
        movement.setOrganizationId(organizationId);
        movement.setBranchId(branchId);
        movement.setWarehouseId(warehouseId);
        movement.setBinLocationId(binLocationId);
        movement.setProductId(productId);
        movement.setMovementType(movementType);
        movement.setReferenceType(referenceType);
        movement.setReferenceId(referenceId);
        movement.setReferenceNumber(referenceNumber);
        movement.setDirection(direction);
        movement.setUomId(uomId);
        movement.setQuantity(quantity);
        movement.setBaseQuantity(baseQuantity);
        movement.setUnitCost(unitCost);
        movement.setTotalCost(unitCost == null ? null : unitCost.multiply(baseQuantity));
        movement.setMovementAt(LocalDateTime.now());
        StockMovement saved = stockMovementRepository.save(movement);

        auditEventWriter.write(
                organizationId,
                branchId,
                "INVENTORY_MOVEMENT",
                "stock_movement",
                saved.getId(),
                referenceNumber,
                direction + "_" + movementType,
                warehouseId,
                null,
                null,
                "Inventory movement posted",
                summaryPayload
        );

        return saved;
    }
}
