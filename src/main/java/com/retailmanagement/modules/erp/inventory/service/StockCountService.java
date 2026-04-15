package com.retailmanagement.modules.erp.inventory.service;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.erp.catalog.entity.StoreProduct;
import com.retailmanagement.modules.erp.catalog.repository.StoreProductRepository;
import com.retailmanagement.modules.erp.common.ErpSecurityUtils;
import com.retailmanagement.modules.erp.common.constants.ErpDocumentStatuses;
import com.retailmanagement.modules.erp.common.security.ErpAccessGuard;
import com.retailmanagement.modules.erp.foundation.entity.Warehouse;
import com.retailmanagement.modules.erp.foundation.repository.WarehouseRepository;
import com.retailmanagement.modules.erp.inventory.dto.InventoryDtos;
import com.retailmanagement.modules.erp.inventory.entity.InventoryBalance;
import com.retailmanagement.modules.erp.inventory.entity.StockAdjustment;
import com.retailmanagement.modules.erp.inventory.entity.StockCountLine;
import com.retailmanagement.modules.erp.inventory.entity.StockCountSession;
import com.retailmanagement.modules.erp.inventory.repository.InventoryBalanceRepository;
import com.retailmanagement.modules.erp.inventory.repository.StockCountLineRepository;
import com.retailmanagement.modules.erp.inventory.repository.StockCountSessionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class StockCountService {

    private final StockCountSessionRepository stockCountSessionRepository;
    private final StockCountLineRepository stockCountLineRepository;
    private final InventoryBalanceRepository inventoryBalanceRepository;
    private final WarehouseRepository warehouseRepository;
    private final StoreProductRepository storeProductRepository;
    private final InventoryOperationsService inventoryOperationsService;
    private final ErpAccessGuard accessGuard;

    @Transactional(readOnly = true)
    public List<InventoryDtos.StockCountSessionResponse> listSessions(Long organizationId, Long warehouseId) {
        accessGuard.assertOrganizationAccess(organizationId);
        List<StockCountSession> sessions = warehouseId == null
                ? stockCountSessionRepository.findByOrganizationIdOrderByCountDateDescIdDesc(organizationId)
                : stockCountSessionRepository.findByOrganizationIdAndWarehouseIdOrderByCountDateDescIdDesc(organizationId, warehouseId);
        return sessions.stream()
                .map(session -> toSessionResponse(session, stockCountLineRepository.findByStockCountSessionIdOrderByIdAsc(session.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public InventoryDtos.StockCountSessionResponse getSession(Long id) {
        StockCountSession session = requireSession(id);
        List<StockCountLine> lines = stockCountLineRepository.findByStockCountSessionIdOrderByIdAsc(id);
        return toSessionResponse(session, lines);
    }

    public InventoryDtos.StockCountSessionResponse createSession(InventoryDtos.CreateStockCountSessionRequest request) {
        Long organizationId = request.organizationId() != null
                ? request.organizationId()
                : ErpSecurityUtils.currentOrganizationId().orElseThrow(() -> new BusinessException("Organization context is required"));
        accessGuard.assertOrganizationAccess(organizationId);

        Warehouse warehouse = warehouseRepository.findByIdAndOrganizationId(request.warehouseId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found: " + request.warehouseId()));
        Long branchId = request.branchId() != null ? request.branchId() : warehouse.getBranchId();
        accessGuard.assertBranchAccess(organizationId, branchId);

        StockCountSession session = new StockCountSession();
        session.setOrganizationId(organizationId);
        session.setBranchId(branchId);
        session.setWarehouseId(warehouse.getId());
        session.setCountNumber("CNT-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + System.currentTimeMillis());
        session.setCountDate(LocalDate.now());
        session.setStatus(ErpDocumentStatuses.DRAFT);
        session.setNotes(trimToNull(request.notes()));
        session.setStartedAt(LocalDateTime.now());
        session = stockCountSessionRepository.save(session);
        return toSessionResponse(session, List.of());
    }

    public InventoryDtos.StockCountSessionResponse upsertLines(Long sessionId, InventoryDtos.UpsertStockCountLinesRequest request) {
        if (request == null || request.lines() == null || request.lines().isEmpty()) {
            throw new BusinessException("At least one stock count line is required");
        }
        StockCountSession session = requireSessionForEditing(sessionId);

        for (InventoryDtos.StockCountLineRequest lineRequest : request.lines()) {
            StoreProduct product = storeProductRepository.findById(lineRequest.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Store product not found: " + lineRequest.productId()));
            if (!session.getOrganizationId().equals(product.getOrganizationId())) {
                throw new BusinessException("Store product does not belong to organization " + session.getOrganizationId());
            }
            BigDecimal counted = requiredQuantity(lineRequest.countedBaseQuantity(), "Counted quantity is required");
            BigDecimal expected = expectedQuantity(session, lineRequest.productId(), lineRequest.batchId());
            BigDecimal variance = counted.subtract(expected);

            StockCountLine line = stockCountLineRepository
                    .findByStockCountSessionIdAndProductIdAndBatchId(sessionId, lineRequest.productId(), lineRequest.batchId())
                    .orElseGet(StockCountLine::new);
            line.setStockCountSessionId(sessionId);
            line.setProductId(lineRequest.productId());
            line.setBatchId(lineRequest.batchId());
            line.setExpectedBaseQuantity(expected);
            line.setCountedBaseQuantity(counted);
            line.setVarianceBaseQuantity(variance);
            line.setRemarks(trimToNull(lineRequest.remarks()));
            stockCountLineRepository.save(line);
        }

        return getSession(sessionId);
    }

    public InventoryDtos.StockCountSessionResponse submitSession(Long sessionId) {
        StockCountSession session = requireSessionForEditing(sessionId);
        List<StockCountLine> lines = stockCountLineRepository.findByStockCountSessionIdOrderByIdAsc(sessionId);
        if (lines.isEmpty()) {
            throw new BusinessException("Cannot submit a stock count session without lines");
        }
        session.setStatus(ErpDocumentStatuses.SUBMITTED);
        session.setSubmittedAt(LocalDateTime.now());
        return toSessionResponse(stockCountSessionRepository.save(session), lines);
    }

    public InventoryDtos.StockCountSessionResponse postVariances(Long sessionId) {
        StockCountSession session = requireSession(sessionId);
        if (!(ErpDocumentStatuses.SUBMITTED.equals(session.getStatus()) || ErpDocumentStatuses.DRAFT.equals(session.getStatus()))) {
            throw new BusinessException("Stock count variances can only be posted from draft or submitted sessions");
        }
        List<StockCountLine> lines = stockCountLineRepository.findByStockCountSessionIdOrderByIdAsc(sessionId);
        if (lines.isEmpty()) {
            throw new BusinessException("Cannot post variances for an empty stock count session");
        }

        for (StockCountLine line : lines) {
            if (line.getPostedAdjustmentId() != null || line.getVarianceBaseQuantity() == null || line.getVarianceBaseQuantity().compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            StoreProduct product = storeProductRepository.findById(line.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Store product not found: " + line.getProductId()));
            StockAdjustment adjustment = inventoryOperationsService.createManualAdjustment(
                    session.getOrganizationId(),
                    session.getBranchId(),
                    session.getWarehouseId(),
                    null,
                    line.getProductId(),
                    product.getBaseUomId(),
                    line.getVarianceBaseQuantity(),
                    line.getVarianceBaseQuantity(),
                    expectedUnitCost(session, line.getProductId(), line.getBatchId()),
                    "Stock count variance for " + session.getCountNumber()
            );
            line.setPostedAdjustmentId(adjustment.getId());
            stockCountLineRepository.save(line);
        }

        session.setStatus(ErpDocumentStatuses.POSTED);
        if (session.getSubmittedAt() == null) {
            session.setSubmittedAt(LocalDateTime.now());
        }
        session.setVariancePostedAt(LocalDateTime.now());
        return toSessionResponse(stockCountSessionRepository.save(session), stockCountLineRepository.findByStockCountSessionIdOrderByIdAsc(sessionId));
    }

    private StockCountSession requireSession(Long sessionId) {
        StockCountSession session = stockCountSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Stock count session not found: " + sessionId));
        accessGuard.assertBranchAccess(session.getOrganizationId(), session.getBranchId());
        return session;
    }

    private StockCountSession requireSessionForEditing(Long sessionId) {
        StockCountSession session = requireSession(sessionId);
        if (ErpDocumentStatuses.POSTED.equals(session.getStatus()) || ErpDocumentStatuses.CANCELLED.equals(session.getStatus())) {
            throw new BusinessException("Stock count session is no longer editable");
        }
        return session;
    }

    private BigDecimal expectedQuantity(StockCountSession session, Long productId, Long batchId) {
        List<InventoryBalance> balances = matchingBalances(session, productId, batchId);
        return balances.stream()
                .map(InventoryBalance::getOnHandBaseQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal expectedUnitCost(StockCountSession session, Long productId, Long batchId) {
        List<InventoryBalance> balances = matchingBalances(session, productId, batchId);
        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;

        for (InventoryBalance balance : balances) {
            BigDecimal quantity = balance.getOnHandBaseQuantity() == null ? BigDecimal.ZERO : balance.getOnHandBaseQuantity();
            BigDecimal unitCost = balance.getAvgCost() == null ? BigDecimal.ZERO : balance.getAvgCost();
            if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            totalQuantity = totalQuantity.add(quantity);
            totalCost = totalCost.add(unitCost.multiply(quantity));
        }

        if (totalQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return totalCost.divide(totalQuantity, 6, RoundingMode.HALF_UP);
    }

    private List<InventoryBalance> matchingBalances(StockCountSession session, Long productId, Long batchId) {
        List<InventoryBalance> balances = inventoryBalanceRepository
                .findByOrganizationIdAndProductIdAndWarehouseId(session.getOrganizationId(), productId, session.getWarehouseId());
        return balances.stream()
                .filter(balance -> batchId == null ? balance.getBatchId() == null : batchId.equals(balance.getBatchId()))
                .toList();
    }

    private BigDecimal requiredQuantity(BigDecimal value, String message) {
        if (value == null) {
            throw new BusinessException(message);
        }
        return value;
    }

    private InventoryDtos.StockCountSessionResponse toSessionResponse(StockCountSession session, List<StockCountLine> lines) {
        List<InventoryDtos.StockCountLineResponse> lineResponses = lines.stream()
                .map(this::toLineResponse)
                .toList();
        long varianceLines = lines.stream()
                .filter(line -> line.getVarianceBaseQuantity() != null && line.getVarianceBaseQuantity().compareTo(BigDecimal.ZERO) != 0)
                .count();
        return new InventoryDtos.StockCountSessionResponse(
                session.getId(),
                session.getOrganizationId(),
                session.getBranchId(),
                session.getWarehouseId(),
                session.getCountNumber(),
                session.getCountDate(),
                session.getStatus(),
                session.getNotes(),
                session.getStartedAt(),
                session.getSubmittedAt(),
                session.getVariancePostedAt(),
                lines.size(),
                varianceLines,
                lineResponses,
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }

    private InventoryDtos.StockCountLineResponse toLineResponse(StockCountLine line) {
        return new InventoryDtos.StockCountLineResponse(
                line.getId(),
                line.getStockCountSessionId(),
                line.getProductId(),
                line.getBatchId(),
                line.getExpectedBaseQuantity(),
                line.getCountedBaseQuantity(),
                line.getVarianceBaseQuantity(),
                line.getPostedAdjustmentId(),
                line.getRemarks(),
                line.getCreatedAt(),
                line.getUpdatedAt()
        );
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
