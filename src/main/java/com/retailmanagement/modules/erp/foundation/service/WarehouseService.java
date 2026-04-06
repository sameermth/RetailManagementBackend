package com.retailmanagement.modules.erp.foundation.service;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.auth.security.UserPrincipal;
import com.retailmanagement.modules.erp.common.ErpSecurityUtils;
import com.retailmanagement.modules.erp.common.security.ErpAccessGuard;
import com.retailmanagement.modules.erp.foundation.dto.WarehouseDtos;
import com.retailmanagement.modules.erp.foundation.entity.Branch;
import com.retailmanagement.modules.erp.foundation.entity.Warehouse;
import com.retailmanagement.modules.erp.foundation.repository.BranchRepository;
import com.retailmanagement.modules.erp.foundation.repository.WarehouseRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final BranchRepository branchRepository;
    private final ErpAccessGuard accessGuard;

    @Transactional(readOnly = true)
    public List<Warehouse> list(Long organizationId, Long branchId) {
        UserPrincipal principal = ErpSecurityUtils.requirePrincipal();
        if (branchId != null) {
            accessGuard.assertBranchAccess(organizationId, branchId);
            return warehouseRepository.findByOrganizationIdAndBranchIdOrderByIdAsc(organizationId, branchId);
        }

        accessGuard.assertOrganizationAccess(organizationId);
        if (principal.hasRole("OWNER")) {
            return warehouseRepository.findByOrganizationIdOrderByBranchIdAscIdAsc(organizationId);
        }
        if (principal.getAccessibleBranchIds() == null || principal.getAccessibleBranchIds().isEmpty()) {
            if (principal.getDefaultBranchId() == null) {
                return List.of();
            }
            return warehouseRepository.findByOrganizationIdAndBranchIdOrderByIdAsc(organizationId, principal.getDefaultBranchId());
        }
        return warehouseRepository.findByOrganizationIdAndBranchIdInOrderByBranchIdAscIdAsc(
                organizationId,
                principal.getAccessibleBranchIds()
        );
    }

    @Transactional(readOnly = true)
    public Warehouse get(Long organizationId, Long warehouseId) {
        Warehouse warehouse = warehouseRepository.findByIdAndOrganizationId(warehouseId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found: " + warehouseId));
        accessGuard.assertBranchAccess(organizationId, warehouse.getBranchId());
        return warehouse;
    }

    public Warehouse create(WarehouseDtos.CreateWarehouseRequest request) {
        Branch branch = requireActiveBranch(request.organizationId(), request.branchId());
        clearPrimaryWarehouseIfNeeded(request.organizationId(), branch.getId(), request.isPrimary());

        Warehouse warehouse = new Warehouse();
        warehouse.setOrganizationId(request.organizationId());
        warehouse.setBranchId(branch.getId());
        warehouse.setCode(request.code());
        warehouse.setName(request.name());
        warehouse.setIsPrimary(Boolean.TRUE.equals(request.isPrimary()));
        if (request.isActive() != null) {
            warehouse.setIsActive(request.isActive());
        }
        return warehouseRepository.save(warehouse);
    }

    public Warehouse update(Long organizationId, Long warehouseId, WarehouseDtos.UpdateWarehouseRequest request) {
        Warehouse warehouse = get(organizationId, warehouseId);
        if (request.code() != null) warehouse.setCode(request.code());
        if (request.name() != null) warehouse.setName(request.name());
        if (request.isPrimary() != null) {
            clearPrimaryWarehouseIfNeeded(organizationId, warehouse.getBranchId(), request.isPrimary());
            warehouse.setIsPrimary(request.isPrimary());
        }
        if (request.isActive() != null) warehouse.setIsActive(request.isActive());
        return warehouseRepository.save(warehouse);
    }

    private Branch requireActiveBranch(Long organizationId, Long branchId) {
        accessGuard.assertBranchAccess(organizationId, branchId);
        Branch branch = branchRepository.findByIdAndOrganizationId(branchId, organizationId)
                .orElseThrow(() -> new BusinessException("Branch does not belong to organization: " + branchId));
        if (Boolean.FALSE.equals(branch.getIsActive())) {
            throw new BusinessException("Branch is inactive: " + branchId);
        }
        return branch;
    }

    private void clearPrimaryWarehouseIfNeeded(Long organizationId, Long branchId, Boolean requestedPrimary) {
        if (!Boolean.TRUE.equals(requestedPrimary)) {
            return;
        }
        warehouseRepository.findByOrganizationIdAndBranchIdAndIsPrimaryTrue(organizationId, branchId)
                .ifPresent(existing -> {
                    existing.setIsPrimary(false);
                    warehouseRepository.save(existing);
                });
    }
}
