package com.retailmanagement.modules.erp.inventory.service;

import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.erp.common.constants.ErpDocumentStatuses;
import com.retailmanagement.modules.erp.common.constants.ErpInventoryMovementTypes;
import com.retailmanagement.modules.erp.common.util.ErpJsonPayloads;
import com.retailmanagement.modules.erp.inventory.entity.StockAdjustment;
import com.retailmanagement.modules.erp.inventory.entity.StockAdjustmentLine;
import com.retailmanagement.modules.erp.inventory.repository.StockAdjustmentLineRepository;
import com.retailmanagement.modules.erp.inventory.repository.StockAdjustmentRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class StockAdjustmentPostingService {

    private final StockAdjustmentRepository stockAdjustmentRepository;
    private final StockAdjustmentLineRepository stockAdjustmentLineRepository;
    private final InventoryPostingService inventoryPostingService;

    public StockAdjustment finalizeApprovedAdjustment(Long stockAdjustmentId) {
        StockAdjustment adjustment = stockAdjustmentRepository.findById(stockAdjustmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Stock adjustment not found: " + stockAdjustmentId));
        if (ErpDocumentStatuses.POSTED.equals(adjustment.getStatus())) {
            return adjustment;
        }

        List<StockAdjustmentLine> lines = stockAdjustmentLineRepository.findByStockAdjustmentId(adjustment.getId());
        for (StockAdjustmentLine line : lines) {
            String direction = line.getBaseQuantityDelta().signum() >= 0 ? "IN" : "OUT";
            String movementType = line.getBaseQuantityDelta().signum() >= 0
                    ? ErpInventoryMovementTypes.ADJUSTMENT_IN
                    : ErpInventoryMovementTypes.ADJUSTMENT_OUT;
            inventoryPostingService.postMovement(
                    adjustment.getOrganizationId(),
                    adjustment.getBranchId(),
                    adjustment.getWarehouseId(),
                    line.getProductId(),
                    null,
                    line.getUomId(),
                    line.getQuantityDelta().abs(),
                    line.getBaseQuantityDelta().abs(),
                    direction,
                    movementType,
                    "stock_adjustment",
                    adjustment.getId(),
                    adjustment.getAdjustmentNumber(),
                    line.getUnitCost(),
                    ErpJsonPayloads.of("reason", line.getLineReason(), "adjustmentNumber", adjustment.getAdjustmentNumber())
            );
        }

        adjustment.setStatus(ErpDocumentStatuses.POSTED);
        return stockAdjustmentRepository.save(adjustment);
    }
}
