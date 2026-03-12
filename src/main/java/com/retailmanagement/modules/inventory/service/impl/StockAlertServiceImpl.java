package com.retailmanagement.modules.inventory.service.impl;

import com.retailmanagement.modules.inventory.dto.response.StockAlertResponse;
import com.retailmanagement.modules.inventory.model.Inventory;
import com.retailmanagement.modules.inventory.repository.InventoryRepository;
import com.retailmanagement.modules.inventory.service.StockAlertService;
import com.retailmanagement.modules.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockAlertServiceImpl implements StockAlertService {

    private final InventoryRepository inventoryRepository;
    private final NotificationService notificationService;

    @Override
    public List<StockAlertResponse> checkAllAlerts() {
        log.debug("Checking all stock alerts");

        List<StockAlertResponse> allAlerts = new ArrayList<>();
        allAlerts.addAll(checkLowStockAlerts());
        allAlerts.addAll(checkOutOfStockAlerts());
        allAlerts.addAll(checkOverStockAlerts());

        return allAlerts;
    }

    @Override
    public List<StockAlertResponse> checkLowStockAlerts() {
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
    public List<StockAlertResponse> checkOutOfStockAlerts() {
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
    public List<StockAlertResponse> checkOverStockAlerts() {
        // Implementation for over stock alerts
        return new ArrayList<>();
    }

    @Override
    public List<StockAlertResponse> checkExpiringSoonAlerts(int days) {
        // Implementation for expiring soon alerts (for perishable items)
        return new ArrayList<>();
    }

    @Override
    @Scheduled(cron = "0 0 8 * * *") // Every day at 8 AM
    public void sendLowStockNotifications() {
        log.info("Scheduled task: Sending low stock notifications");

        List<StockAlertResponse> lowStockAlerts = checkLowStockAlerts();

        for (StockAlertResponse alert : lowStockAlerts) {
            String message = String.format(
                    "Low Stock Alert: %s (%s) has only %d units left in %s. Minimum stock level is %d.",
                    alert.getProductName(),
                    alert.getProductSku(),
                    alert.getCurrentStock(),
                    alert.getWarehouse(),
                    alert.getMinimumStock()
            );

            // Send notification to inventory managers
            notificationService.sendNotificationToRole("INVENTORY_MANAGER",
                    "Low Stock Alert", message);
        }

        log.info("Sent {} low stock notifications", lowStockAlerts.size());
    }

    @Override
    @Scheduled(cron = "0 0 8 * * *") // Every day at 8 AM
    public void sendOutOfStockNotifications() {
        log.info("Scheduled task: Sending out of stock notifications");

        List<StockAlertResponse> outOfStockAlerts = checkOutOfStockAlerts();

        for (StockAlertResponse alert : outOfStockAlerts) {
            String message = String.format(
                    "Out of Stock Alert: %s (%s) is out of stock in %s. Please reorder immediately.",
                    alert.getProductName(),
                    alert.getProductSku(),
                    alert.getWarehouse()
            );

            // Send notification to inventory managers and purchasers
            notificationService.sendNotificationToRole("INVENTORY_MANAGER",
                    "Out of Stock Alert", message);
            notificationService.sendNotificationToRole("PURCHASE_MANAGER",
                    "Out of Stock Alert", message);
        }

        log.info("Sent {} out of stock notifications", outOfStockAlerts.size());
    }
}