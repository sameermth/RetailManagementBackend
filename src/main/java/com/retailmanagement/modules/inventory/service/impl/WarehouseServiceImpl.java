package com.retailmanagement.modules.inventory.service.impl;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.inventory.dto.request.WarehouseRequest;
import com.retailmanagement.modules.inventory.dto.response.WarehouseResponse;
import com.retailmanagement.modules.inventory.mapper.WarehouseMapper;
import com.retailmanagement.modules.inventory.model.Warehouse;
import com.retailmanagement.modules.inventory.repository.WarehouseRepository;
import com.retailmanagement.modules.inventory.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final WarehouseMapper warehouseMapper;

    @Override
    public WarehouseResponse createWarehouse(WarehouseRequest request) {
        log.info("Creating new warehouse with code: {}", request.getCode());

        if (warehouseRepository.existsByCode(request.getCode())) {
            throw new BusinessException("Warehouse with code " + request.getCode() + " already exists");
        }

        // If this is set as primary, unset any existing primary
        if (request.getIsPrimary() != null && request.getIsPrimary()) {
            warehouseRepository.findByIsPrimaryTrue()
                    .ifPresent(primary -> {
                        primary.setIsPrimary(false);
                        warehouseRepository.save(primary);
                    });
        }

        Warehouse warehouse = warehouseMapper.toEntity(request);
        Warehouse savedWarehouse = warehouseRepository.save(warehouse);
        log.info("Warehouse created successfully with ID: {}", savedWarehouse.getId());

        return warehouseMapper.toResponse(savedWarehouse);
    }

    @Override
    public WarehouseResponse updateWarehouse(Long id, WarehouseRequest request) {
        log.info("Updating warehouse with ID: {}", id);

        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + id));

        // Check code uniqueness if changed
        if (!warehouse.getCode().equals(request.getCode()) &&
                warehouseRepository.existsByCode(request.getCode())) {
            throw new BusinessException("Warehouse with code " + request.getCode() + " already exists");
        }

        // Handle primary warehouse change
        if (request.getIsPrimary() != null && request.getIsPrimary() && !warehouse.getIsPrimary()) {
            warehouseRepository.findByIsPrimaryTrue()
                    .ifPresent(primary -> {
                        primary.setIsPrimary(false);
                        warehouseRepository.save(primary);
                    });
        }

        // Update fields
        warehouse.setCode(request.getCode());
        warehouse.setName(request.getName());
        warehouse.setDescription(request.getDescription());
        warehouse.setAddress(request.getAddress());
        warehouse.setCity(request.getCity());
        warehouse.setState(request.getState());
        warehouse.setCountry(request.getCountry());
        warehouse.setPincode(request.getPincode());
        warehouse.setPhone(request.getPhone());
        warehouse.setEmail(request.getEmail());
        warehouse.setManager(request.getManager());
        warehouse.setLatitude(request.getLatitude());
        warehouse.setLongitude(request.getLongitude());
        warehouse.setIsActive(request.getIsActive());
        warehouse.setIsPrimary(request.getIsPrimary());
        warehouse.setCapacity(request.getCapacity());

        Warehouse updatedWarehouse = warehouseRepository.save(warehouse);
        log.info("Warehouse updated successfully with ID: {}", updatedWarehouse.getId());

        return warehouseMapper.toResponse(updatedWarehouse);
    }

    @Override
    public WarehouseResponse getWarehouseById(Long id) {
        log.debug("Fetching warehouse with ID: {}", id);

        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + id));

        return warehouseMapper.toResponse(warehouse);
    }

    @Override
    public WarehouseResponse getWarehouseByCode(String code) {
        log.debug("Fetching warehouse with code: {}", code);

        Warehouse warehouse = warehouseRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with code: " + code));

        return warehouseMapper.toResponse(warehouse);
    }

    @Override
    public List<WarehouseResponse> getAllWarehouses() {
        log.debug("Fetching all warehouses");

        return warehouseRepository.findAll().stream()
                .map(warehouseMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<WarehouseResponse> getActiveWarehouses() {
        log.debug("Fetching active warehouses");

        return warehouseRepository.findByIsActiveTrue().stream()
                .map(warehouseMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteWarehouse(Long id) {
        log.info("Deleting warehouse with ID: {}", id);

        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + id));

        // Check if warehouse has inventory
        if (warehouse.getInventories() != null && !warehouse.getInventories().isEmpty()) {
            throw new BusinessException("Cannot delete warehouse with existing inventory");
        }

        warehouseRepository.delete(warehouse);
        log.info("Warehouse deleted successfully with ID: {}", id);
    }

    @Override
    public void activateWarehouse(Long id) {
        log.info("Activating warehouse with ID: {}", id);

        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + id));

        warehouse.setIsActive(true);
        warehouseRepository.save(warehouse);
    }

    @Override
    public void deactivateWarehouse(Long id) {
        log.info("Deactivating warehouse with ID: {}", id);

        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + id));

        warehouse.setIsActive(false);
        warehouseRepository.save(warehouse);
    }

    @Override
    public WarehouseResponse getPrimaryWarehouse() {
        log.debug("Fetching primary warehouse");

        Warehouse warehouse = warehouseRepository.findByIsPrimaryTrue()
                .orElseThrow(() -> new ResourceNotFoundException("No primary warehouse found"));

        return warehouseMapper.toResponse(warehouse);
    }

    @Override
    public boolean isWarehouseCodeUnique(String code) {
        return !warehouseRepository.existsByCode(code);
    }
}