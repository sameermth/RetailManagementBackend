package com.retailmanagement.modules.inventory.service;

import com.retailmanagement.modules.inventory.dto.request.WarehouseRequest;
import com.retailmanagement.modules.inventory.dto.response.WarehouseResponse;

import java.util.List;

public interface WarehouseService {

    WarehouseResponse createWarehouse(WarehouseRequest request);

    WarehouseResponse updateWarehouse(Long id, WarehouseRequest request);

    WarehouseResponse getWarehouseById(Long id);

    WarehouseResponse getWarehouseByCode(String code);

    List<WarehouseResponse> getAllWarehouses();

    List<WarehouseResponse> getActiveWarehouses();

    void deleteWarehouse(Long id);

    void activateWarehouse(Long id);

    void deactivateWarehouse(Long id);

    WarehouseResponse getPrimaryWarehouse();

    boolean isWarehouseCodeUnique(String code);
}