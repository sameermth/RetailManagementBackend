package com.retailmanagement.modules.erp.sales.repository;

import com.retailmanagement.modules.erp.sales.entity.ProductOwnership;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductOwnershipRepository extends JpaRepository<ProductOwnership, Long> {
    List<ProductOwnership> findByCustomerIdOrderByIdDesc(Long customerId);
    List<ProductOwnership> findBySalesInvoiceLineId(Long salesInvoiceLineId);
    List<ProductOwnership> findByOrganizationId(Long organizationId);
    Optional<ProductOwnership> findByIdAndOrganizationId(Long id, Long organizationId);
}
