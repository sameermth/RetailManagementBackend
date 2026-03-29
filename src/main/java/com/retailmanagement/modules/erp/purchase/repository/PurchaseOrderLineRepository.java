package com.retailmanagement.modules.erp.purchase.repository;

import com.retailmanagement.modules.erp.purchase.entity.PurchaseOrderLine;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseOrderLineRepository extends JpaRepository<PurchaseOrderLine, Long> {
    List<PurchaseOrderLine> findByPurchaseOrderIdOrderByIdAsc(Long purchaseOrderId);
    List<PurchaseOrderLine> findByPurchaseOrderIdInAndProductId(List<Long> purchaseOrderIds, Long productId);
}
