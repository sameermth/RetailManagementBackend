package com.retailmanagement.modules.erp.purchase.repository;

import com.retailmanagement.modules.erp.purchase.entity.PurchaseOrderSupplierAccess;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseOrderSupplierAccessRepository extends JpaRepository<PurchaseOrderSupplierAccess, Long> {
    Optional<PurchaseOrderSupplierAccess> findByPurchaseOrderId(Long purchaseOrderId);
    Optional<PurchaseOrderSupplierAccess> findByAccessToken(String accessToken);
}
