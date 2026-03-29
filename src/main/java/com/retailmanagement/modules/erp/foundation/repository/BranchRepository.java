package com.retailmanagement.modules.erp.foundation.repository;

import com.retailmanagement.modules.erp.foundation.entity.Branch;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchRepository extends JpaRepository<Branch, Long> {
    Optional<Branch> findByIdAndOrganizationId(Long id, Long organizationId);
    List<Branch> findByOrganizationIdOrderByIdAsc(Long organizationId);
    List<Branch> findByOrganizationIdAndIsActiveTrueOrderByIdAsc(Long organizationId);
}
