package com.retailmanagement.modules.inventory.service.impl;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.inventory.dto.request.StockMovementRequest;
import com.retailmanagement.modules.inventory.dto.response.StockMovementResponse;
import com.retailmanagement.modules.inventory.mapper.StockMovementMapper;
import com.retailmanagement.modules.inventory.model.Inventory;
import com.retailmanagement.modules.inventory.model.StockMovement;
import com.retailmanagement.modules.inventory.model.Warehouse;
import com.retailmanagement.modules.inventory.repository.InventoryRepository;
import com.retailmanagement.modules.inventory.repository.StockMovementRepository;
import com.retailmanagement.modules.inventory.repository.WarehouseRepository;
import com.retailmanagement.modules.inventory.service.StockMovementService;
import com.retailmanagement.modules.product.model.Product;
import com.retailmanagement.modules.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class StockMovementServiceImpl implements StockMovementService {

    private final StockMovementRepository stockMovementRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryRepository inventoryRepository;
    private final StockMovementMapper stockMovementMapper;

    @Override
    public StockMovementResponse createStockMovement(StockMovementRequest request) {
        log.info("Creating stock movement for product ID: {}", request.getProductId());

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));

        Warehouse fromWarehouse = null;
        if (request.getFromWarehouseId() != null) {
            fromWarehouse = warehouseRepository.findById(request.getFromWarehouseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Source warehouse not found"));
        }

        Warehouse toWarehouse = null;
        if (request.getToWarehouseId() != null) {
            toWarehouse = warehouseRepository.findById(request.getToWarehouseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Destination warehouse not found"));
        }

        // Validate inventory and update stock based on movement type
        updateInventoryBasedOnMovement(request, product, fromWarehouse, toWarehouse);

        StockMovement stockMovement = stockMovementMapper.toEntity(request);
        stockMovement.setReferenceNumber(generateReferenceNumber());
        stockMovement.setProduct(product);
        stockMovement.setFromWarehouse(fromWarehouse);
        stockMovement.setToWarehouse(toWarehouse);
        stockMovement.setPerformedBy("SYSTEM"); // In real app, get from SecurityContext

        StockMovement savedMovement = stockMovementRepository.save(stockMovement);
        log.info("Stock movement created successfully with reference: {}", savedMovement.getReferenceNumber());

        return stockMovementMapper.toResponse(savedMovement);
    }

    private void updateInventoryBasedOnMovement(StockMovementRequest request, Product product,
                                                Warehouse fromWarehouse, Warehouse toWarehouse) {
        switch (request.getMovementType()) {
            case PURCHASE_RECEIVED:
                if (toWarehouse != null) {
                    addStock(product, toWarehouse, request.getQuantity());
                }
                break;
            case SALES_ISSUED:
                if (fromWarehouse != null) {
                    removeStock(product, fromWarehouse, request.getQuantity());
                }
                break;
            case TRANSFER_OUT:
                if (fromWarehouse != null) {
                    removeStock(product, fromWarehouse, request.getQuantity());
                }
                break;
            case TRANSFER_IN:
                if (toWarehouse != null) {
                    addStock(product, toWarehouse, request.getQuantity());
                }
                break;
            default:
                // Other movement types don't automatically update stock
                break;
        }
    }

    private void addStock(Product product, Warehouse warehouse, Integer quantity) {
        Inventory inventory = inventoryRepository.findByProductAndWarehouse(product, warehouse)
                .orElseGet(() -> {
                    Inventory newInventory = Inventory.builder()
                            .product(product)
                            .warehouse(warehouse)
                            .quantity(0)
                            .reservedQuantity(0)
                            .availableQuantity(0)
                            .build();
                    return inventoryRepository.save(newInventory);
                });

        inventory.setQuantity(inventory.getQuantity() + quantity);
        inventory.calculateAvailableQuantity();
        inventory.setLastMovementDate(LocalDateTime.now());
        inventoryRepository.save(inventory);
    }

    private void removeStock(Product product, Warehouse warehouse, Integer quantity) {
        Inventory inventory = inventoryRepository.findByProductAndWarehouse(product, warehouse)
                .orElseThrow(() -> new BusinessException("Inventory not found for product in specified warehouse"));

        if (inventory.getAvailableQuantity() < quantity) {
            throw new BusinessException("Insufficient stock available");
        }

        inventory.setQuantity(inventory.getQuantity() - quantity);
        inventory.calculateAvailableQuantity();
        inventory.setLastMovementDate(LocalDateTime.now());
        inventoryRepository.save(inventory);
    }

    private String generateReferenceNumber() {
        String reference;
        do {
            reference = "MOV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (stockMovementRepository.existsByReferenceNumber(reference));
        return reference;
    }

    @Override
    public StockMovementResponse getStockMovementById(Long id) {
        log.debug("Fetching stock movement with ID: {}", id);

        StockMovement stockMovement = stockMovementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Stock movement not found with id: " + id));

        return stockMovementMapper.toResponse(stockMovement);
    }

    @Override
    public StockMovementResponse getStockMovementByReferenceNumber(String referenceNumber) {
        log.debug("Fetching stock movement with reference: {}", referenceNumber);

        StockMovement stockMovement = stockMovementRepository.findByReferenceNumber(referenceNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Stock movement not found with reference: " + referenceNumber));

        return stockMovementMapper.toResponse(stockMovement);
    }

    @Override
    public Page<StockMovementResponse> getAllStockMovements(Pageable pageable) {
        log.debug("Fetching all stock movements with pagination");

        return stockMovementRepository.findAll(pageable)
                .map(stockMovementMapper::toResponse);
    }

    @Override
    public List<StockMovementResponse> getStockMovementsByProduct(Long productId) {
        log.debug("Fetching stock movements for product ID: {}", productId);

        return stockMovementRepository.findByProductIdOrderByMovementDateDesc(productId).stream()
                .map(stockMovementMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Page<StockMovementResponse> getStockMovementsByProduct(Long productId, Pageable pageable) {
        log.debug("Fetching stock movements for product ID: {} with pagination", productId);

        return stockMovementRepository.findByProductId(productId, pageable)
                .map(stockMovementMapper::toResponse);
    }

    @Override
    public List<StockMovementResponse> getStockMovementsByWarehouse(Long warehouseId) {
        log.debug("Fetching stock movements for warehouse ID: {}", warehouseId);

        return stockMovementRepository.findByFromWarehouseIdOrToWarehouseId(warehouseId, warehouseId).stream()
                .map(stockMovementMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<StockMovementResponse> getStockMovementsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Fetching stock movements between {} and {}", startDate, endDate);

        return stockMovementRepository.findByDateRange(startDate, endDate).stream()
                .map(stockMovementMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<StockMovementResponse> getStockMovementsByReference(String referenceType, Long referenceId) {
        log.debug("Fetching stock movements for reference type: {} and ID: {}", referenceType, referenceId);

        return stockMovementRepository.findByReference(referenceType, referenceId).stream()
                .map(stockMovementMapper::toResponse)
                .collect(Collectors.toList());
    }
}