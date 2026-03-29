package com.retailmanagement.modules.erp.foundation.service;

import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.erp.common.security.ErpAccessGuard;
import com.retailmanagement.modules.erp.foundation.dto.BranchDtos;
import com.retailmanagement.modules.erp.foundation.entity.Branch;
import com.retailmanagement.modules.erp.foundation.repository.BranchRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class BranchService {

    private final BranchRepository branchRepository;
    private final ErpAccessGuard accessGuard;

    @Transactional(readOnly = true)
    public List<Branch> list(Long organizationId) {
        accessGuard.assertOrganizationAccess(organizationId);
        return branchRepository.findByOrganizationIdOrderByIdAsc(organizationId);
    }

    @Transactional(readOnly = true)
    public Branch get(Long organizationId, Long branchId) {
        accessGuard.assertBranchAccess(organizationId, branchId);
        return branchRepository.findByIdAndOrganizationId(branchId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + branchId));
    }

    public Branch create(BranchDtos.CreateBranchRequest request) {
        accessGuard.assertOrganizationAccess(request.organizationId());
        Branch branch = new Branch();
        branch.setOrganizationId(request.organizationId());
        branch.setCode(request.code());
        branch.setName(request.name());
        branch.setPhone(request.phone());
        branch.setEmail(request.email());
        branch.setAddressLine1(request.addressLine1());
        branch.setAddressLine2(request.addressLine2());
        branch.setCity(request.city());
        branch.setState(request.state());
        branch.setPostalCode(request.postalCode());
        branch.setCountry(request.country());
        if (request.isActive() != null) {
            branch.setIsActive(request.isActive());
        }
        return branchRepository.save(branch);
    }

    public Branch update(Long organizationId, Long branchId, BranchDtos.UpdateBranchRequest request) {
        Branch branch = get(organizationId, branchId);
        if (request.code() != null) branch.setCode(request.code());
        if (request.name() != null) branch.setName(request.name());
        if (request.phone() != null) branch.setPhone(request.phone());
        if (request.email() != null) branch.setEmail(request.email());
        if (request.addressLine1() != null) branch.setAddressLine1(request.addressLine1());
        if (request.addressLine2() != null) branch.setAddressLine2(request.addressLine2());
        if (request.city() != null) branch.setCity(request.city());
        if (request.state() != null) branch.setState(request.state());
        if (request.postalCode() != null) branch.setPostalCode(request.postalCode());
        if (request.country() != null) branch.setCountry(request.country());
        if (request.isActive() != null) branch.setIsActive(request.isActive());
        return branchRepository.save(branch);
    }
}
