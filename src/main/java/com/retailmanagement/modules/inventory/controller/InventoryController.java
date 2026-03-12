package com.retailmanagement.modules.inventory.controller;

import com.retailmanagement.modules.inventory.dto.request.InventoryRequest;
import com.retailmanagement.modules.inventory.dto.request.StockAdjustmentRequest;
import com.retailmanagement.modules.inventory.dto.request.StockTransferRequest;
import com.retailmanagement.modules.inventory.dto.response.InventoryResponse;
import com.retailmanagement.modules.inventory.dto.response.StockAlertResponse;
import com.retailmanagement.modules.inventory.service.InventoryService;
import com.retailmanagement.modules.inventory.service.StockAlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory", description = "Inventory management endpoints")
public class InventoryController {

    private final InventoryService inventoryService;
    private final StockAlertService stockAlertService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('INVENTORY_MANAGER')")
    @Operation(summary = "Create inventory record")
    public ResponseEntity<InventoryResponse> createInventory(@Valid @RequestBody InventoryRequest request) {
        return new ResponseEntity<>(inventoryService.createInventory(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('INVENTORY_MANAGER')")
    @Operation(summary = "Update inventory record")
    public ResponseEntity<InventoryResponse> updateInventory(
            @PathVariable Long id,
            @Valid @RequestBody InventoryRequest request) {
        return ResponseEntity.ok(inventoryService.updateInventory(id, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get inventory by ID")
    public ResponseEntity<InventoryResponse> getInventoryById(@PathVariable Long id) {
        return ResponseEntity.ok(inventoryService.getInventoryById(id));
    }

    @GetMapping("/product/{productId}/warehouse/{warehouseId}")
    @Operation(summary = "Get inventory by product and warehouse")
    public ResponseEntity<InventoryResponse> getInventoryByProductAndWarehouse(
            @PathVariable Long productId,
            @PathVariable Long warehouseId) {
        return ResponseEntity.ok(inventoryService.getInventoryByProductAndWarehouse(productId, warehouseId));
    }

    @GetMapping
    @Operation(summary = "Get all inventory with pagination")
    public ResponseEntity<Page<InventoryResponse>> getAllInventory(
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(inventoryService.getAllInventory(pageable));
    }

    @GetMapping("/product/{productId}")
    @Operation(summary = "Get inventory by product")
    public ResponseEntity<List<InventoryResponse>> getInventoryByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(inventoryService.getInventoryByProduct(productId));
    }

    @GetMapping("/warehouse/{warehouseId}")
    @Operation(summary = "Get inventory by warehouse")
    public ResponseEntity<List<InventoryResponse>> getInventoryByWarehouse(@PathVariable Long warehouseId) {
        return ResponseEntity.ok(inventoryService.getInventoryByWarehouse(warehouseId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete inventory record")
    public ResponseEntity<Void> deleteInventory(@PathVariable Long id) {
        inventoryService.deleteInventory(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/adjust")
    @PreAuthorize("hasRole('ADMIN') or hasRole('INVENTORY_MANAGER')")
    @Operation(summary = "Adjust stock quantity")
    public ResponseEntity<InventoryResponse> adjustStock(@Valid @RequestBody StockAdjustmentRequest request) {
        return ResponseEntity.ok(inventoryService.adjustStock(request));
    }

    @PostMapping("/transfer")
    @PreAuthorize("hasRole('ADMIN') or hasRole('INVENTORY_MANAGER')")
    @Operation(summary = "Transfer stock between warehouses")
    public ResponseEntity<InventoryResponse> transferStock(@Valid @RequestBody StockTransferRequest request) {
        return ResponseEntity.ok(inventoryService.transferStock(request));
    }

    @GetMapping("/stock/{productId}/total")
    @Operation(summary = "Get total stock by product across all warehouses")
    public ResponseEntity<Integer> getTotalStockByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(inventoryService.getTotalStockByProduct(productId));
    }

    @GetMapping("/check-availability")
    @Operation(summary = "Check stock availability")
    public ResponseEntity<Boolean> checkStockAvailability(
            @RequestParam Long productId,
            @RequestParam Long warehouseId,
            @RequestParam Integer quantity) {
        return ResponseEntity.ok(inventoryService.checkStockAvailability(productId, warehouseId, quantity));
    }

    @GetMapping("/alerts/low-stock")
    @Operation(summary = "Get low stock alerts")
    public ResponseEntity<List<StockAlertResponse>> getLowStockAlerts() {
        return ResponseEntity.ok(inventoryService.getLowStockAlerts());
    }

    @GetMapping("/alerts/out-of-stock")
    @Operation(summary = "Get out of stock alerts")
    public ResponseEntity<List<StockAlertResponse>> getOutOfStockAlerts() {
        return ResponseEntity.ok(inventoryService.getOutOfStockAlerts());
    }

    @GetMapping("/alerts/all")
    @Operation(summary = "Get all stock alerts")
    public ResponseEntity<List<StockAlertResponse>> getAllAlerts() {
        return ResponseEntity.ok(stockAlertService.checkAllAlerts());
    }

    @GetMapping("/stats/low-stock-count")
    @Operation(summary = "Get low stock count")
    public ResponseEntity<Long> getLowStockCount() {
        return ResponseEntity.ok(inventoryService.getLowStockCount());
    }

    @GetMapping("/stats/out-of-stock-count")
    @Operation(summary = "Get out of stock count")
    public ResponseEntity<Long> getOutOfStockCount() {
        return ResponseEntity.ok(inventoryService.getOutOfStockCount());
    }
}