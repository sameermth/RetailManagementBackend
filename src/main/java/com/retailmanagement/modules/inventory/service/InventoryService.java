package com.retailmanagement.modules.inventory.service;

import com.retailmanagement.modules.inventory.dto.request.InventoryRequest;
import com.retailmanagement.modules.inventory.dto.request.StockAdjustmentRequest;
import com.retailmanagement.modules.inventory.dto.request.StockTransferRequest;
import com.retailmanagement.modules.inventory.dto.response.InventoryResponse;
import com.retailmanagement.modules.inventory.dto.response.StockAlertResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface InventoryService {

    InventoryResponse createInventory(InventoryRequest request);

    InventoryResponse updateInventory(Long id, InventoryRequest request);

    InventoryResponse getInventoryById(Long id);

    InventoryResponse getInventoryByProductAndWarehouse(Long productId, Long warehouseId);

    Page<InventoryResponse> getAllInventory(Pageable pageable);

    List<InventoryResponse> getInventoryByProduct(Long productId);

    List<InventoryResponse> getInventoryByWarehouse(Long warehouseId);

    void deleteInventory(Long id);

    InventoryResponse adjustStock(StockAdjustmentRequest request);

    InventoryResponse transferStock(StockTransferRequest request);

    Integer getTotalStockByProduct(Long productId);

    boolean checkStockAvailability(Long productId, Long warehouseId, Integer quantity);

    void reserveStock(Long productId, Long warehouseId, Integer quantity);

    void releaseReservedStock(Long productId, Long warehouseId, Integer quantity);

    List<StockAlertResponse> getLowStockAlerts();

    List<StockAlertResponse> getOutOfStockAlerts();

    long getLowStockCount();

    long getOutOfStockCount();
}