package com.retailmanagement.modules.erp.catalog.repository;

import com.retailmanagement.modules.erp.catalog.entity.TaxGroup;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxGroupRepository extends JpaRepository<TaxGroup, Long> {
    Optional<TaxGroup> findByIdAndOrganizationId(Long id, Long organizationId);
}
