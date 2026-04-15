package com.retailmanagement.modules.erp.purchase.repository;

import com.retailmanagement.modules.erp.purchase.entity.PurchaseOrderSupplierDispatchNotice;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseOrderSupplierDispatchNoticeRepository extends JpaRepository<PurchaseOrderSupplierDispatchNotice, Long> {
    List<PurchaseOrderSupplierDispatchNotice> findByPurchaseOrderIdOrderByDispatchDateDescIdDesc(Long purchaseOrderId);
}
