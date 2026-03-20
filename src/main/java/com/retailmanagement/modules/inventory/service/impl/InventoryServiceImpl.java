package com.retailmanagement.modules.inventory.service.impl;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.inventory.dto.request.InventoryRequest;
import com.retailmanagement.modules.inventory.dto.request.StockAdjustmentRequest;
import com.retailmanagement.modules.inventory.dto.request.StockTransferRequest;
import com.retailmanagement.modules.inventory.dto.response.InventoryResponse;
import com.retailmanagement.modules.inventory.dto.response.StockAlertResponse;
import com.retailmanagement.modules.inventory.enums.MovementType;
import com.retailmanagement.modules.inventory.mapper.InventoryMapper;
import com.retailmanagement.modules.inventory.model.Inventory;
import com.retailmanagement.modules.inventory.model.StockMovement;
import com.retailmanagement.modules.inventory.model.Warehouse;
import com.retailmanagement.modules.inventory.repository.InventoryRepository;
import com.retailmanagement.modules.inventory.repository.StockMovementRepository;
import com.retailmanagement.modules.inventory.repository.WarehouseRepository;
import com.retailmanagement.modules.inventory.service.InventoryService;
import com.retailmanagement.modules.product.model.Product;
import com.retailmanagement.modules.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final StockMovementRepository stockMovementRepository;
    private final InventoryMapper inventoryMapper;

    @Override
    public InventoryResponse createInventory(InventoryRequest request) {
        log.info("Creating inventory for product ID: {} in warehouse ID: {}",
                request.getProductId(), request.getWarehouseId());

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));

        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + request.getWarehouseId()));

        // Check if inventory already exists
        inventoryRepository.findByProductAndWarehouse(product, warehouse)
                .ifPresent(i -> {
                    throw new BusinessException("Inventory already exists for this product and warehouse");
                });

        Inventory inventory = inventoryMapper.toEntity(request);
        inventory.setProduct(product);
        inventory.setWarehouse(warehouse);
        inventory.setReservedQuantity(0);
        inventory.setAvailableQuantity(request.getQuantity() != null ? request.getQuantity() : 0);

        Inventory savedInventory = inventoryRepository.save(inventory);

        // Create initial stock movement
        if (request.getQuantity() != null && request.getQuantity() > 0) {
            createStockMovement(
                    product,
                    null,
                    warehouse,
                    MovementType.INITIAL_STOCK,
                    request.getQuantity(),
                    0,
                    request.getQuantity(),
                    "INITIAL_STOCK",
                    null,
                    "Initial stock setup"
            );
        }

        log.info("Inventory created successfully with ID: {}", savedInventory.getId());

        return inventoryMapper.toResponse(savedInventory);
    }

    @Override
    public InventoryResponse updateInventory(Long id, InventoryRequest request) {
        log.info("Updating inventory with ID: {}", id);

        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found with id: " + id));

        // Update fields
        inventory.setMinimumStock(request.getMinimumStock());
        inventory.setMaximumStock(request.getMaximumStock());
        inventory.setReorderPoint(request.getReorderPoint());
        inventory.setReorderQuantity(request.getReorderQuantity());
        inventory.setBinLocation(request.getBinLocation());
        inventory.setShelfNumber(request.getShelfNumber());
        inventory.setAverageCost(BigDecimal.valueOf(request.getAverageCost()));
        inventory.setLastPurchasePrice(BigDecimal.valueOf(request.getLastPurchasePrice()));

        // If quantity is being updated directly (should be rare, use stock movements instead)
        if (request.getQuantity() != null && !request.getQuantity().equals(inventory.getQuantity())) {
            int oldQuantity = inventory.getQuantity();
            int difference = request.getQuantity() - oldQuantity;

            inventory.setQuantity(request.getQuantity());
            inventory.calculateAvailableQuantity();

            // Log the adjustment
            createStockMovement(
                    inventory.getProduct(),
                    inventory.getWarehouse(),
                    inventory.getWarehouse(),
                    difference > 0 ? MovementType.ADJUSTMENT_ADD : MovementType.ADJUSTMENT_REMOVE,
                    Math.abs(difference),
                    oldQuantity,
                    request.getQuantity(),
                    "MANUAL_ADJUSTMENT",
                    null,
                    "Manual inventory adjustment"
            );
        }

        Inventory updatedInventory = inventoryRepository.save(inventory);
        log.info("Inventory updated successfully with ID: {}", updatedInventory.getId());

        return inventoryMapper.toResponse(updatedInventory);
    }

    @Override
    public InventoryResponse getInventoryById(Long id) {
        log.debug("Fetching inventory with ID: {}", id);

        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found with id: " + id));

        return inventoryMapper.toResponse(inventory);
    }

    @Override
    public InventoryResponse getInventoryByProductAndWarehouse(Long productId, Long warehouseId) {
        log.debug("Fetching inventory for product ID: {} and warehouse ID: {}", productId, warehouseId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + warehouseId));

        Inventory inventory = inventoryRepository.findByProductAndWarehouse(product, warehouse)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for this product and warehouse"));

        return inventoryMapper.toResponse(inventory);
    }

    @Override
    public Page<InventoryResponse> getAllInventory(Pageable pageable) {
        log.debug("Fetching all inventory with pagination");

        return inventoryRepository.findAll(pageable)
                .map(inventoryMapper::toResponse);
    }

    @Override
    public List<InventoryResponse> getInventoryByProduct(Long productId) {
        log.debug("Fetching inventory for product ID: {}", productId);

        return inventoryRepository.findByProductId(productId).stream()
                .map(inventoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<InventoryResponse> getInventoryByWarehouse(Long warehouseId) {
        log.debug("Fetching inventory for warehouse ID: {}", warehouseId);

        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + warehouseId));

        return inventoryRepository.findByWarehouse(warehouse).stream()
                .map(inventoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteInventory(Long id) {
        log.info("Deleting inventory with ID: {}", id);

        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found with id: " + id));

        inventoryRepository.delete(inventory);
        log.info("Inventory deleted successfully with ID: {}", id);
    }

    @Override
    public InventoryResponse adjustStock(StockAdjustmentRequest request) {
        log.info("Adjusting stock for product ID: {} in warehouse ID: {}",
                request.getProductId(), request.getWarehouseId());

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));

        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + request.getWarehouseId()));

        Inventory inventory = inventoryRepository.findByProductAndWarehouse(product, warehouse)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for this product and warehouse"));

        int oldQuantity = inventory.getQuantity();
        int newQuantity = request.getNewQuantity();
        int difference = newQuantity - oldQuantity;

        if (difference == 0) {
            throw new BusinessException("New quantity is same as current quantity");
        }

        inventory.setQuantity(newQuantity);
        inventory.calculateAvailableQuantity();
        inventory.setLastMovementDate(LocalDateTime.now());

        Inventory updatedInventory = inventoryRepository.save(inventory);

        // Create stock movement
        createStockMovement(
                product,
                warehouse,
                warehouse,
                difference > 0 ? MovementType.ADJUSTMENT_ADD : MovementType.ADJUSTMENT_REMOVE,
                Math.abs(difference),
                oldQuantity,
                newQuantity,
                "STOCK_ADJUSTMENT",
                null,
                request.getReason() != null ? request.getReason() : "Stock adjustment"
        );

        log.info("Stock adjusted successfully for inventory ID: {}", updatedInventory.getId());

        return inventoryMapper.toResponse(updatedInventory);
    }

    @Override
    public InventoryResponse transferStock(StockTransferRequest request) {
        log.info("Transferring stock for product ID: {} from warehouse ID: {} to warehouse ID: {}",
                request.getProductId(), request.getFromWarehouseId(), request.getToWarehouseId());

        if (request.getFromWarehouseId().equals(request.getToWarehouseId())) {
            throw new BusinessException("Source and destination warehouses cannot be the same");
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));

        Warehouse fromWarehouse = warehouseRepository.findById(request.getFromWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Source warehouse not found with id: " + request.getFromWarehouseId()));

        Warehouse toWarehouse = warehouseRepository.findById(request.getToWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Destination warehouse not found with id: " + request.getToWarehouseId()));

        Inventory fromInventory = inventoryRepository.findByProductAndWarehouse(product, fromWarehouse)
                .orElseThrow(() -> new ResourceNotFoundException("Source inventory not found"));

        // Check if enough stock is available
        if (fromInventory.getAvailableQuantity() < request.getQuantity()) {
            throw new BusinessException("Insufficient stock available for transfer. Available: " +
                    fromInventory.getAvailableQuantity() + ", Requested: " + request.getQuantity());
        }

        // Get or create destination inventory
        Inventory toInventory = inventoryRepository.findByProductAndWarehouse(product, toWarehouse)
                .orElseGet(() -> {
                    Inventory newInventory = Inventory.builder()
                            .product(product)
                            .warehouse(toWarehouse)
                            .quantity(0)
                            .reservedQuantity(0)
                            .availableQuantity(0)
                            .minimumStock(fromInventory.getMinimumStock())
                            .maximumStock(fromInventory.getMaximumStock())
                            .reorderPoint(fromInventory.getReorderPoint())
                            .reorderQuantity(fromInventory.getReorderQuantity())
                            .build();
                    return inventoryRepository.save(newInventory);
                });

        // Update source inventory
        int fromOldQuantity = fromInventory.getQuantity();
        fromInventory.setQuantity(fromOldQuantity - request.getQuantity());
        fromInventory.calculateAvailableQuantity();
        fromInventory.setLastMovementDate(LocalDateTime.now());

        // Update destination inventory
        int toOldQuantity = toInventory.getQuantity();
        toInventory.setQuantity(toOldQuantity + request.getQuantity());
        toInventory.calculateAvailableQuantity();
        toInventory.setLastMovementDate(LocalDateTime.now());

        inventoryRepository.save(fromInventory);
        inventoryRepository.save(toInventory);

        // Create stock movements
        String referenceNumber = UUID.randomUUID().toString();

        // Outbound movement from source
        createStockMovement(
                product,
                fromWarehouse,
                toWarehouse,
                MovementType.TRANSFER_OUT,
                request.getQuantity(),
                fromOldQuantity,
                fromInventory.getQuantity(),
                "STOCK_TRANSFER",
                referenceNumber,
                request.getReason() != null ? request.getReason() : "Stock transfer"
        );

        // Inbound movement to destination
        createStockMovement(
                product,
                fromWarehouse,
                toWarehouse,
                MovementType.TRANSFER_IN,
                request.getQuantity(),
                toOldQuantity,
                toInventory.getQuantity(),
                "STOCK_TRANSFER",
                referenceNumber,
                request.getReason() != null ? request.getReason() : "Stock transfer"
        );

        log.info("Stock transferred successfully");

        return inventoryMapper.toResponse(toInventory);
    }

    @Override
    public Integer getTotalStockByProduct(Long productId) {
        return inventoryRepository.getTotalStockByProduct(productId);
    }

    @Override
    public boolean checkStockAvailability(Long productId, Long warehouseId, Integer quantity) {
        log.debug("Checking stock availability for product ID: {}, warehouse ID: {}, quantity: {}",
                productId, warehouseId, quantity);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + warehouseId));

        Inventory inventory = inventoryRepository.findByProductAndWarehouse(product, warehouse)
                .orElse(null);

        return inventory != null && inventory.getAvailableQuantity() >= quantity;
    }

    @Override
    @Transactional
    public void reserveStock(Long productId, Long warehouseId, Integer quantity) {
        log.debug("Reserving stock for product ID: {}, warehouse ID: {}, quantity: {}",
                productId, warehouseId, quantity);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + warehouseId));

        Inventory inventory = inventoryRepository.findByProductAndWarehouse(product, warehouse)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found"));

        if (inventory.getAvailableQuantity() < quantity) {
            throw new BusinessException("Insufficient stock to reserve. Available: " +
                    inventory.getAvailableQuantity() + ", Requested: " + quantity);
        }

        inventory.setReservedQuantity(inventory.getReservedQuantity() + quantity);
        inventory.calculateAvailableQuantity();
        inventoryRepository.save(inventory);
    }

    @Override
    @Transactional
    public void releaseReservedStock(Long productId, Long warehouseId, Integer quantity) {
        log.debug("Releasing reserved stock for product ID: {}, warehouse ID: {}, quantity: {}",
                productId, warehouseId, quantity);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + warehouseId));

        Inventory inventory = inventoryRepository.findByProductAndWarehouse(product, warehouse)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found"));

        if (inventory.getReservedQuantity() < quantity) {
            throw new BusinessException("Cannot release more than reserved quantity");
        }

        inventory.setReservedQuantity(inventory.getReservedQuantity() - quantity);
        inventory.calculateAvailableQuantity();
        inventoryRepository.save(inventory);
    }

    @Override
    public List<StockAlertResponse> getLowStockAlerts() {
        log.debug("Fetching low stock alerts");

        List<Inventory> lowStockInventory = inventoryRepository.findLowStockInventory();
        List<StockAlertResponse> alerts = new ArrayList<>();

        for (Inventory inventory : lowStockInventory) {
            alerts.add(StockAlertResponse.builder()
                    .productId(inventory.getProduct().getId())
                    .productName(inventory.getProduct().getName())
                    .productSku(inventory.getProduct().getSku())
                    .category(inventory.getProduct().getCategory() != null ?
                            inventory.getProduct().getCategory().getName() : null)
                    .warehouse(inventory.getWarehouse().getName())
                    .currentStock(inventory.getQuantity())
                    .minimumStock(inventory.getMinimumStock())
                    .reorderPoint(inventory.getReorderPoint())
                    .recommendedOrder(inventory.getReorderQuantity())
                    .alertType("LOW_STOCK")
                    .severity(inventory.getQuantity() <= 0 ? "HIGH" : "MEDIUM")
                    .message("Stock level is below minimum threshold")
                    .build());
        }

        return alerts;
    }

    @Override
    public List<StockAlertResponse> getOutOfStockAlerts() {
        log.debug("Fetching out of stock alerts");

        List<Inventory> outOfStockInventory = inventoryRepository.findOutOfStockInventory();
        List<StockAlertResponse> alerts = new ArrayList<>();

        for (Inventory inventory : outOfStockInventory) {
            alerts.add(StockAlertResponse.builder()
                    .productId(inventory.getProduct().getId())
                    .productName(inventory.getProduct().getName())
                    .productSku(inventory.getProduct().getSku())
                    .category(inventory.getProduct().getCategory() != null ?
                            inventory.getProduct().getCategory().getName() : null)
                    .warehouse(inventory.getWarehouse().getName())
                    .currentStock(0)
                    .minimumStock(inventory.getMinimumStock())
                    .reorderPoint(inventory.getReorderPoint())
                    .recommendedOrder(inventory.getReorderQuantity())
                    .alertType("OUT_OF_STOCK")
                    .severity("HIGH")
                    .message("Product is out of stock")
                    .build());
        }

        return alerts;
    }

    @Override
    public long getLowStockCount() {
        return inventoryRepository.countLowStock();
    }

    @Override
    public long getOutOfStockCount() {
        return inventoryRepository.countOutOfStock();
    }

    private void createStockMovement(Product product, Warehouse fromWarehouse, Warehouse toWarehouse,
                                     MovementType type, int quantity, int previousStock, int newStock,
                                     String referenceType, String referenceNumber, String reason) {

        StockMovement movement = StockMovement.builder()
                .referenceNumber(referenceNumber != null ? referenceNumber : UUID.randomUUID().toString())
                .product(product)
                .fromWarehouse(fromWarehouse)
                .toWarehouse(toWarehouse)
                .movementType(type)
                .quantity(quantity)
                .previousStock(previousStock)
                .newStock(newStock)
                .unitCost(product.getCostPrice())
                .totalCost(product.getCostPrice() != null ? product.getCostPrice().multiply(new java.math.BigDecimal(quantity)) : null)
                .referenceType(referenceType)
                .reason(reason)
                .performedBy("SYSTEM") // In real app, get from SecurityContext
                .movementDate(LocalDateTime.now())
                .build();

        stockMovementRepository.save(movement);
    }

    @Override
    public void removeStock(Long productId, long quantity, Long warehouseId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        // Find inventory and update stock
        List<Inventory> inventories = inventoryRepository.findByProductId(productId);
        if (inventories.isEmpty()) {
            throw new RuntimeException("No inventory found for product");
        }
        
        Inventory inventory = inventories.getFirst();
        if (inventory.getAvailableQuantity() < quantity) {
            throw new RuntimeException("Insufficient stock");
        }
        
        inventory.setQuantity((int) (inventory.getQuantity() - quantity));
        inventory.setAvailableQuantity(inventory.getAvailableQuantity() - (int) quantity);
        inventoryRepository.save(inventory);
    }

    @Override
    public void addStock(Long productId, long quantity, Long warehouseId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        // Find inventory and update stock
        List<Inventory> inventories = inventoryRepository.findByProductId(productId);
        if (inventories.isEmpty()) {
            // if no inventory exists for product, create inventory
            Inventory inventory = Inventory.builder()
                    .product(product)
                    .warehouse(Warehouse.builder().id(warehouseId).build()) // or assign to a default warehouse
                    .quantity(0)
                    .availableQuantity( 0)
                    .reservedQuantity(0)
                    .build();

            inventories.add(inventory);
        }
        
        Inventory inventory = inventories.getFirst();
        inventory.setQuantity((int) (inventory.getQuantity() + quantity));
        inventory.setAvailableQuantity(inventory.getAvailableQuantity() + (int) quantity);
        inventoryRepository.save(inventory);
    }
}

