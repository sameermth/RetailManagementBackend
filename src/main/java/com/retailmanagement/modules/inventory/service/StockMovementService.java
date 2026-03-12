package com.retailmanagement.modules.inventory.service;

import com.retailmanagement.modules.inventory.dto.request.StockMovementRequest;
import com.retailmanagement.modules.inventory.dto.response.StockMovementResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface StockMovementService {

    StockMovementResponse createStockMovement(StockMovementRequest request);

    StockMovementResponse getStockMovementById(Long id);

    StockMovementResponse getStockMovementByReferenceNumber(String referenceNumber);

    Page<StockMovementResponse> getAllStockMovements(Pageable pageable);

    List<StockMovementResponse> getStockMovementsByProduct(Long productId);

    Page<StockMovementResponse> getStockMovementsByProduct(Long productId, Pageable pageable);

    List<StockMovementResponse> getStockMovementsByWarehouse(Long warehouseId);

    List<StockMovementResponse> getStockMovementsByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    List<StockMovementResponse> getStockMovementsByReference(String referenceType, Long referenceId);
}