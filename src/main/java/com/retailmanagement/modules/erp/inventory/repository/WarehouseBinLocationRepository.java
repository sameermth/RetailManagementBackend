package com.retailmanagement.modules.erp.inventory.repository;

import com.retailmanagement.modules.erp.inventory.entity.WarehouseBinLocation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WarehouseBinLocationRepository extends JpaRepository<WarehouseBinLocation, Long> {
    List<WarehouseBinLocation> findByOrganizationIdAndWarehouseIdOrderByIsDefaultDescSortOrderAscCodeAsc(Long organizationId, Long warehouseId);
    List<WarehouseBinLocation> findByOrganizationIdAndWarehouseIdAndIsActiveTrueOrderByIsDefaultDescSortOrderAscCodeAsc(Long organizationId, Long warehouseId);
    Optional<WarehouseBinLocation> findByIdAndOrganizationId(Long id, Long organizationId);
    Optional<WarehouseBinLocation> findByOrganizationIdAndWarehouseIdAndCodeIgnoreCase(Long organizationId, Long warehouseId, String code);
    long countByOrganizationIdAndWarehouseIdAndIsDefaultTrue(Long organizationId, Long warehouseId);
}
