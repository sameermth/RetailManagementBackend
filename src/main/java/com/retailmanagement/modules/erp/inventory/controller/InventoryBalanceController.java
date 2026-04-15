package com.retailmanagement.modules.erp.inventory.controller;

import com.retailmanagement.modules.erp.common.api.ErpApiResponse;
import com.retailmanagement.modules.erp.inventory.dto.InventoryDtos;
import com.retailmanagement.modules.erp.inventory.entity.InventoryBalance;
import com.retailmanagement.modules.erp.inventory.repository.InventoryBalanceRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/erp/inventory-balances") @RequiredArgsConstructor
@Tag(name = "ERP Inventory Balances", description = "ERP inventory balance query endpoints")
public class InventoryBalanceController {
 private final InventoryBalanceRepository repository;
 @GetMapping("/warehouse/{warehouseId}") @Operation(summary = "List inventory balances by warehouse")
 @PreAuthorize("hasAuthority('inventory.view')")
 public ErpApiResponse<List<InventoryDtos.InventoryBalanceResponse>> byWarehouse(@RequestParam Long organizationId, @PathVariable Long warehouseId){
   return ErpApiResponse.ok(repository.findByOrganizationIdAndWarehouseId(organizationId, warehouseId).stream().map(this::toResponse).toList());
 }
 @GetMapping("/product/{productId}") @Operation(summary = "List inventory balances by product")
 @PreAuthorize("hasAuthority('inventory.view')")
 public ErpApiResponse<List<InventoryDtos.InventoryBalanceResponse>> byProduct(@RequestParam Long organizationId, @PathVariable Long productId){
   return ErpApiResponse.ok(repository.findByOrganizationIdAndProductId(organizationId, productId).stream().map(this::toResponse).toList());
 }
 @GetMapping("/bin/{binLocationId}") @Operation(summary = "List inventory balances by bin")
 @PreAuthorize("hasAuthority('inventory.view')")
 public ErpApiResponse<List<InventoryDtos.InventoryBalanceResponse>> byBin(@RequestParam Long organizationId, @PathVariable Long binLocationId){
   return ErpApiResponse.ok(repository.findByOrganizationIdAndBinLocationId(organizationId, binLocationId).stream().map(this::toResponse).toList());
 }

 private InventoryDtos.InventoryBalanceResponse toResponse(InventoryBalance balance) {
  return new InventoryDtos.InventoryBalanceResponse(balance.getId(), balance.getOrganizationId(), balance.getBranchId(),
          balance.getWarehouseId(), balance.getBinLocationId(), balance.getProductId(), balance.getBatchId(), balance.getOnHandBaseQuantity(),
          balance.getReservedBaseQuantity(), balance.getAvailableBaseQuantity(), balance.getAvgCost(),
          balance.getCreatedAt(), balance.getUpdatedAt());
 }
}
