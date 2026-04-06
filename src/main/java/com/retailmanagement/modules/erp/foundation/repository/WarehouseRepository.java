package com.retailmanagement.modules.erp.foundation.repository;

import com.retailmanagement.modules.erp.foundation.entity.Warehouse;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
    Optional<Warehouse> findByIdAndOrganizationId(Long id, Long organizationId);
    List<Warehouse> findByOrganizationIdOrderByBranchIdAscIdAsc(Long organizationId);
    List<Warehouse> findByOrganizationIdAndBranchIdOrderByIdAsc(Long organizationId, Long branchId);
    List<Warehouse> findByOrganizationIdAndBranchIdInOrderByBranchIdAscIdAsc(Long organizationId, Collection<Long> branchIds);
    Optional<Warehouse> findByOrganizationIdAndBranchIdAndIsPrimaryTrue(Long organizationId, Long branchId);
}
