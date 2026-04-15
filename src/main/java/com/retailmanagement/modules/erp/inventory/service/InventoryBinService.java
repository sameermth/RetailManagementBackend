package com.retailmanagement.modules.erp.inventory.service;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.erp.foundation.entity.Warehouse;
import com.retailmanagement.modules.erp.foundation.repository.WarehouseRepository;
import com.retailmanagement.modules.erp.inventory.dto.InventoryDtos;
import com.retailmanagement.modules.erp.inventory.entity.WarehouseBinLocation;
import com.retailmanagement.modules.erp.inventory.repository.WarehouseBinLocationRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class InventoryBinService {

    private final WarehouseRepository warehouseRepository;
    private final WarehouseBinLocationRepository warehouseBinLocationRepository;

    @Transactional(readOnly = true)
    public List<InventoryDtos.WarehouseBinLocationResponse> list(Long organizationId, Long warehouseId, Boolean activeOnly) {
        List<WarehouseBinLocation> bins = Boolean.TRUE.equals(activeOnly)
                ? warehouseBinLocationRepository.findByOrganizationIdAndWarehouseIdAndIsActiveTrueOrderByIsDefaultDescSortOrderAscCodeAsc(organizationId, warehouseId)
                : warehouseBinLocationRepository.findByOrganizationIdAndWarehouseIdOrderByIsDefaultDescSortOrderAscCodeAsc(organizationId, warehouseId);
        return bins.stream().map(this::toResponse).toList();
    }

    public InventoryDtos.WarehouseBinLocationResponse create(InventoryDtos.CreateWarehouseBinLocationRequest request) {
        Warehouse warehouse = warehouseRepository.findByIdAndOrganizationId(request.warehouseId(), request.organizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found: " + request.warehouseId()));

        warehouseBinLocationRepository.findByOrganizationIdAndWarehouseIdAndCodeIgnoreCase(
                request.organizationId(),
                request.warehouseId(),
                request.code()
        ).ifPresent(existing -> {
            throw new BusinessException("Bin code already exists for this warehouse: " + request.code());
        });

        if (Boolean.TRUE.equals(request.isDefault())) {
            clearDefaultBins(request.organizationId(), request.warehouseId());
        }

        WarehouseBinLocation bin = new WarehouseBinLocation();
        bin.setOrganizationId(request.organizationId());
        bin.setBranchId(warehouse.getBranchId());
        bin.setWarehouseId(request.warehouseId());
        bin.setCode(request.code().trim().toUpperCase());
        bin.setName(request.name().trim());
        bin.setZoneCode(trimToNull(request.zoneCode()));
        bin.setSortOrder(request.sortOrder());
        bin.setIsDefault(Boolean.TRUE.equals(request.isDefault()));
        bin.setIsActive(request.isActive() == null || Boolean.TRUE.equals(request.isActive()));
        return toResponse(warehouseBinLocationRepository.save(bin));
    }

    public InventoryDtos.WarehouseBinLocationResponse update(Long id, InventoryDtos.UpdateWarehouseBinLocationRequest request) {
        WarehouseBinLocation bin = warehouseBinLocationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse bin not found: " + id));

        String nextCode = request.code().trim().toUpperCase();
        warehouseBinLocationRepository.findByOrganizationIdAndWarehouseIdAndCodeIgnoreCase(
                bin.getOrganizationId(),
                bin.getWarehouseId(),
                nextCode
        ).ifPresent(existing -> {
            if (!existing.getId().equals(bin.getId())) {
                throw new BusinessException("Bin code already exists for this warehouse: " + nextCode);
            }
        });

        if (Boolean.TRUE.equals(request.isDefault())) {
            clearDefaultBins(bin.getOrganizationId(), bin.getWarehouseId());
        }

        bin.setCode(nextCode);
        bin.setName(request.name().trim());
        bin.setZoneCode(trimToNull(request.zoneCode()));
        bin.setSortOrder(request.sortOrder());
        bin.setIsDefault(Boolean.TRUE.equals(request.isDefault()));
        bin.setIsActive(request.isActive() == null || Boolean.TRUE.equals(request.isActive()));
        return toResponse(warehouseBinLocationRepository.save(bin));
    }

    @Transactional(readOnly = true)
    public WarehouseBinLocation requireActiveBin(Long organizationId, Long warehouseId, Long binLocationId) {
        WarehouseBinLocation bin = warehouseBinLocationRepository.findByIdAndOrganizationId(binLocationId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse bin not found: " + binLocationId));
        if (!bin.getWarehouseId().equals(warehouseId)) {
            throw new BusinessException("Bin does not belong to warehouse " + warehouseId + ": " + binLocationId);
        }
        if (!Boolean.TRUE.equals(bin.getIsActive())) {
            throw new BusinessException("Bin is inactive: " + bin.getCode());
        }
        return bin;
    }

    private void clearDefaultBins(Long organizationId, Long warehouseId) {
        if (warehouseBinLocationRepository.countByOrganizationIdAndWarehouseIdAndIsDefaultTrue(organizationId, warehouseId) == 0) {
            return;
        }
        warehouseBinLocationRepository.findByOrganizationIdAndWarehouseIdOrderByIsDefaultDescSortOrderAscCodeAsc(organizationId, warehouseId)
                .forEach(bin -> {
                    if (Boolean.TRUE.equals(bin.getIsDefault())) {
                        bin.setIsDefault(false);
                        warehouseBinLocationRepository.save(bin);
                    }
                });
    }

    private InventoryDtos.WarehouseBinLocationResponse toResponse(WarehouseBinLocation bin) {
        return new InventoryDtos.WarehouseBinLocationResponse(
                bin.getId(),
                bin.getOrganizationId(),
                bin.getBranchId(),
                bin.getWarehouseId(),
                bin.getCode(),
                bin.getName(),
                bin.getZoneCode(),
                bin.getSortOrder(),
                bin.getIsDefault(),
                bin.getIsActive(),
                bin.getCreatedAt(),
                bin.getUpdatedAt()
        );
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
