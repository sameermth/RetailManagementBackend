package com.retailmanagement.modules.erp.service.repository;

import com.retailmanagement.modules.erp.service.entity.WarrantyExtension;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WarrantyExtensionRepository extends JpaRepository<WarrantyExtension, Long> {
    List<WarrantyExtension> findByOrganizationIdAndProductOwnershipIdOrderByIdAsc(Long organizationId, Long productOwnershipId);
    Optional<WarrantyExtension> findByIdAndOrganizationId(Long id, Long organizationId);
}
