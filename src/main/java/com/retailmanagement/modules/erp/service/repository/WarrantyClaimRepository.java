package com.retailmanagement.modules.erp.service.repository;

import com.retailmanagement.modules.erp.service.entity.WarrantyClaim;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WarrantyClaimRepository extends JpaRepository<WarrantyClaim, Long> {
    List<WarrantyClaim> findTop100ByOrganizationIdOrderByClaimDateDescIdDesc(Long organizationId);
    Optional<WarrantyClaim> findByOrganizationIdAndId(Long organizationId, Long id);
}
