package com.retailmanagement.modules.erp.inventory.service;

import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.erp.common.constants.ErpDocumentStatuses;
import com.retailmanagement.modules.erp.common.constants.ErpInventoryMovementTypes;
import com.retailmanagement.modules.erp.common.util.ErpJsonPayloads;
import com.retailmanagement.modules.erp.inventory.entity.StockTransfer;
import com.retailmanagement.modules.erp.inventory.entity.StockTransferLine;
import com.retailmanagement.modules.erp.inventory.repository.StockTransferLineRepository;
import com.retailmanagement.modules.erp.inventory.repository.StockTransferRepository;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class StockTransferPostingService {

    private final StockTransferRepository stockTransferRepository;
    private final StockTransferLineRepository stockTransferLineRepository;
    private final InventoryPostingService inventoryPostingService;

    public StockTransfer finalizeApprovedTransfer(Long stockTransferId) {
        StockTransfer transfer = stockTransferRepository.findById(stockTransferId)
                .orElseThrow(() -> new ResourceNotFoundException("Stock transfer not found: " + stockTransferId));
        if (ErpDocumentStatuses.POSTED.equals(transfer.getStatus())) {
            return transfer;
        }

        List<StockTransferLine> lines = stockTransferLineRepository.findByStockTransferId(transfer.getId());
        for (StockTransferLine line : lines) {
            String payload = ErpJsonPayloads.of(
                    "transferNumber", transfer.getTransferNumber(),
                    "productId", line.getProductId(),
                    "fromWarehouseId", transfer.getFromWarehouseId(),
                    "toWarehouseId", transfer.getToWarehouseId()
            );

            inventoryPostingService.postMovement(
                    transfer.getOrganizationId(),
                    transfer.getBranchId(),
                    transfer.getFromWarehouseId(),
                    line.getProductId(),
                    null,
                    line.getUomId(),
                    line.getQuantity(),
                    line.getBaseQuantity(),
                    "OUT",
                    ErpInventoryMovementTypes.TRANSFER_OUT,
                    "stock_transfer",
                    transfer.getId(),
                    transfer.getTransferNumber(),
                    BigDecimal.ZERO,
                    payload
            );

            inventoryPostingService.postMovement(
                    transfer.getOrganizationId(),
                    transfer.getBranchId(),
                    transfer.getToWarehouseId(),
                    line.getProductId(),
                    null,
                    line.getUomId(),
                    line.getQuantity(),
                    line.getBaseQuantity(),
                    "IN",
                    ErpInventoryMovementTypes.TRANSFER_IN,
                    "stock_transfer",
                    transfer.getId(),
                    transfer.getTransferNumber(),
                    BigDecimal.ZERO,
                    payload
            );
        }

        transfer.setStatus(ErpDocumentStatuses.POSTED);
        return stockTransferRepository.save(transfer);
    }
}
