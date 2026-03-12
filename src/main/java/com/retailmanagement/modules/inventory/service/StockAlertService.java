package com.retailmanagement.modules.inventory.service;

import com.retailmanagement.modules.inventory.dto.response.StockAlertResponse;

import java.util.List;

public interface StockAlertService {

    List<StockAlertResponse> checkAllAlerts();

    List<StockAlertResponse> checkLowStockAlerts();

    List<StockAlertResponse> checkOutOfStockAlerts();

    List<StockAlertResponse> checkOverStockAlerts();

    List<StockAlertResponse> checkExpiringSoonAlerts(int days);

    void sendLowStockNotifications();

    void sendOutOfStockNotifications();
}