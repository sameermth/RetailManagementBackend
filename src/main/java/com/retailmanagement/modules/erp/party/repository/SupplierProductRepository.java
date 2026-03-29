package com.retailmanagement.modules.erp.party.repository;

import com.retailmanagement.modules.erp.party.entity.SupplierProduct;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository("erpSupplierProductRepository")
public interface SupplierProductRepository extends JpaRepository<SupplierProduct, Long> {
    List<SupplierProduct> findByOrganizationIdAndProductIdAndIsActiveTrueOrderByIsPreferredDescPriorityAscIdAsc(Long organizationId, Long productId);
    List<SupplierProduct> findByOrganizationIdAndSupplierIdAndIsActiveTrueOrderByIsPreferredDescPriorityAscIdAsc(Long organizationId, Long supplierId);
    List<SupplierProduct> findByOrganizationIdAndSupplierIdAndProductIdAndIsActiveTrueOrderByIsPreferredDescPriorityAscIdAsc(Long organizationId, Long supplierId, Long productId);
    Optional<SupplierProduct> findByIdAndOrganizationId(Long id, Long organizationId);
}
