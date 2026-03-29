package com.retailmanagement.modules.erp.inventory.service;

import com.retailmanagement.modules.erp.approval.dto.ErpApprovalDtos;
import com.retailmanagement.modules.erp.approval.service.ErpApprovalService;
import com.retailmanagement.modules.erp.common.constants.ErpDocumentStatuses;
import com.retailmanagement.modules.erp.common.constants.ErpInventoryMovementTypes;
import com.retailmanagement.modules.erp.common.util.ErpJsonPayloads;
import com.retailmanagement.modules.erp.inventory.entity.StockAdjustment;
import com.retailmanagement.modules.erp.inventory.entity.StockAdjustmentLine;
import com.retailmanagement.modules.erp.inventory.entity.StockTransfer;
import com.retailmanagement.modules.erp.inventory.entity.StockTransferLine;
import com.retailmanagement.modules.erp.inventory.repository.StockAdjustmentLineRepository;
import com.retailmanagement.modules.erp.inventory.repository.StockAdjustmentRepository;
import com.retailmanagement.modules.erp.inventory.repository.StockTransferLineRepository;
import com.retailmanagement.modules.erp.inventory.repository.StockTransferRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class InventoryOperationsService {

    private final InventoryPostingService inventoryPostingService;
    private final StockTransferRepository stockTransferRepository;
    private final StockTransferLineRepository stockTransferLineRepository;
    private final StockAdjustmentRepository stockAdjustmentRepository;
    private final StockAdjustmentLineRepository stockAdjustmentLineRepository;
    private final StockAdjustmentPostingService stockAdjustmentPostingService;
    private final StockTransferPostingService stockTransferPostingService;
    private final ErpApprovalService erpApprovalService;

    public StockAdjustment createManualAdjustment(
            Long organizationId,
            Long branchId,
            Long warehouseId,
            Long productId,
            Long uomId,
            BigDecimal quantityDelta,
            BigDecimal baseQuantityDelta,
            BigDecimal unitCost,
            String reason
    ) {
        String adjustmentNumber = "ADJ-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + System.currentTimeMillis();
        StockAdjustment adjustment = new StockAdjustment();
        adjustment.setOrganizationId(organizationId);
        adjustment.setBranchId(branchId);
        adjustment.setWarehouseId(warehouseId);
        adjustment.setAdjustmentNumber(adjustmentNumber);
        adjustment.setAdjustmentDate(LocalDate.now());
        adjustment.setReason(reason);
        adjustment.setStatus(ErpDocumentStatuses.SUBMITTED);
        adjustment = stockAdjustmentRepository.save(adjustment);

        StockAdjustmentLine line = new StockAdjustmentLine();
        line.setStockAdjustmentId(adjustment.getId());
        line.setProductId(productId);
        line.setUomId(uomId);
        line.setQuantityDelta(quantityDelta);
        line.setBaseQuantityDelta(baseQuantityDelta);
        line.setUnitCost(unitCost);
        line.setLineReason(reason);
        stockAdjustmentLineRepository.save(line);

        ErpApprovalService.ApprovalEvaluation evaluation = erpApprovalService.evaluate(
                organizationId,
                new ErpApprovalDtos.ApprovalEvaluationQuery("stock_adjustment", adjustment.getId(), "MANUAL_STOCK_ADJUSTMENT")
        );
        if (evaluation.approvalRequired()) {
            adjustment.setStatus(ErpDocumentStatuses.PENDING_APPROVAL);
            adjustment = stockAdjustmentRepository.save(adjustment);
            if (!evaluation.pendingRequestExists()) {
                erpApprovalService.createRequest(
                        organizationId,
                        branchId,
                        new ErpApprovalDtos.CreateApprovalRequest(
                                "stock_adjustment",
                                adjustment.getId(),
                                adjustment.getAdjustmentNumber(),
                                "MANUAL_STOCK_ADJUSTMENT",
                                "Manual stock adjustment matched approval rule",
                                null,
                                null
                        )
                );
            }
            return adjustment;
        }

        return stockAdjustmentPostingService.finalizeApprovedAdjustment(adjustment.getId());
    }

    public StockTransfer createTransfer(
            Long organizationId,
            Long branchId,
            Long fromWarehouseId,
            Long toWarehouseId,
            List<TransferLineCommand> lines
    ) {
        String transferNumber = "TRF-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + System.currentTimeMillis();
        StockTransfer transfer = new StockTransfer();
        transfer.setOrganizationId(organizationId);
        transfer.setBranchId(branchId);
        transfer.setFromWarehouseId(fromWarehouseId);
        transfer.setToWarehouseId(toWarehouseId);
        transfer.setTransferNumber(transferNumber);
        transfer.setTransferDate(LocalDate.now());
        transfer.setStatus(ErpDocumentStatuses.SUBMITTED);
        transfer = stockTransferRepository.save(transfer);

        for (TransferLineCommand command : lines) {
            StockTransferLine line = new StockTransferLine();
            line.setStockTransferId(transfer.getId());
            line.setProductId(command.productId());
            line.setUomId(command.uomId());
            line.setQuantity(command.quantity());
            line.setBaseQuantity(command.baseQuantity());
            stockTransferLineRepository.save(line);
        }

        ErpApprovalService.ApprovalEvaluation evaluation = erpApprovalService.evaluate(
                organizationId,
                new ErpApprovalDtos.ApprovalEvaluationQuery("stock_transfer", transfer.getId(), "STOCK_TRANSFER")
        );
        if (evaluation.approvalRequired()) {
            transfer.setStatus(ErpDocumentStatuses.PENDING_APPROVAL);
            transfer = stockTransferRepository.save(transfer);
            if (!evaluation.pendingRequestExists()) {
                erpApprovalService.createRequest(
                        organizationId,
                        branchId,
                        new ErpApprovalDtos.CreateApprovalRequest(
                                "stock_transfer",
                                transfer.getId(),
                                transfer.getTransferNumber(),
                                "STOCK_TRANSFER",
                                "Stock transfer matched approval rule",
                                null,
                                null
                        )
                );
            }
            return transfer;
        }

        return stockTransferPostingService.finalizeApprovedTransfer(transfer.getId());
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public record TransferLineCommand(
            Long productId,
            Long uomId,
            BigDecimal quantity,
            BigDecimal baseQuantity
    ) {}
}
