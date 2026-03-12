package com.retailmanagement.modules.inventory.controller;

import com.retailmanagement.modules.inventory.dto.request.StockMovementRequest;
import com.retailmanagement.modules.inventory.dto.response.StockMovementResponse;
import com.retailmanagement.modules.inventory.service.StockMovementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/stock-movements")
@RequiredArgsConstructor
@Tag(name = "Stock Movements", description = "Stock movement tracking endpoints")
public class StockMovementController {

    private final StockMovementService stockMovementService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('INVENTORY_MANAGER')")
    @Operation(summary = "Create a stock movement")
    public ResponseEntity<StockMovementResponse> createStockMovement(@Valid @RequestBody StockMovementRequest request) {
        return new ResponseEntity<>(stockMovementService.createStockMovement(request), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get stock movement by ID")
    public ResponseEntity<StockMovementResponse> getStockMovementById(@PathVariable Long id) {
        return ResponseEntity.ok(stockMovementService.getStockMovementById(id));
    }

    @GetMapping("/reference/{referenceNumber}")
    @Operation(summary = "Get stock movement by reference number")
    public ResponseEntity<StockMovementResponse> getStockMovementByReferenceNumber(@PathVariable String referenceNumber) {
        return ResponseEntity.ok(stockMovementService.getStockMovementByReferenceNumber(referenceNumber));
    }

    @GetMapping
    @Operation(summary = "Get all stock movements with pagination")
    public ResponseEntity<Page<StockMovementResponse>> getAllStockMovements(
            @PageableDefault(size = 20, sort = "movementDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(stockMovementService.getAllStockMovements(pageable));
    }

    @GetMapping("/product/{productId}")
    @Operation(summary = "Get stock movements by product")
    public ResponseEntity<List<StockMovementResponse>> getStockMovementsByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(stockMovementService.getStockMovementsByProduct(productId));
    }

    @GetMapping("/product/{productId}/paged")
    @Operation(summary = "Get stock movements by product with pagination")
    public ResponseEntity<Page<StockMovementResponse>> getStockMovementsByProductPaged(
            @PathVariable Long productId,
            @PageableDefault(size = 20, sort = "movementDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(stockMovementService.getStockMovementsByProduct(productId, pageable));
    }

    @GetMapping("/warehouse/{warehouseId}")
    @Operation(summary = "Get stock movements by warehouse")
    public ResponseEntity<List<StockMovementResponse>> getStockMovementsByWarehouse(@PathVariable Long warehouseId) {
        return ResponseEntity.ok(stockMovementService.getStockMovementsByWarehouse(warehouseId));
    }

    @GetMapping("/date-range")
    @Operation(summary = "Get stock movements by date range")
    public ResponseEntity<List<StockMovementResponse>> getStockMovementsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(stockMovementService.getStockMovementsByDateRange(startDate, endDate));
    }

    @GetMapping("/reference")
    @Operation(summary = "Get stock movements by reference")
    public ResponseEntity<List<StockMovementResponse>> getStockMovementsByReference(
            @RequestParam String referenceType,
            @RequestParam Long referenceId) {
        return ResponseEntity.ok(stockMovementService.getStockMovementsByReference(referenceType, referenceId));
    }
}