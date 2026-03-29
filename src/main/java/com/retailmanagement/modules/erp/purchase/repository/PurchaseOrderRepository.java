package com.retailmanagement.modules.erp.purchase.repository;

import com.retailmanagement.modules.erp.purchase.entity.PurchaseOrder;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    List<PurchaseOrder> findTop100ByOrganizationIdOrderByPoDateDescIdDesc(Long organizationId);
    List<PurchaseOrder> findByOrganizationIdOrderByPoDateDescIdDesc(Long organizationId);
    List<PurchaseOrder> findByOrganizationIdAndSupplierIdAndStatusInOrderByPoDateDescIdDesc(Long organizationId, Long supplierId, List<String> statuses);
}
