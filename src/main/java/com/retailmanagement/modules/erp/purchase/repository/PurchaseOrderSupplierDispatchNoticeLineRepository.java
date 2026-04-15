package com.retailmanagement.modules.erp.purchase.repository;

import com.retailmanagement.modules.erp.purchase.entity.PurchaseOrderSupplierDispatchNoticeLine;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseOrderSupplierDispatchNoticeLineRepository extends JpaRepository<PurchaseOrderSupplierDispatchNoticeLine, Long> {
    List<PurchaseOrderSupplierDispatchNoticeLine> findByDispatchNoticeIdOrderByIdAsc(Long dispatchNoticeId);
    List<PurchaseOrderSupplierDispatchNoticeLine> findByPurchaseOrderLineIdIn(List<Long> purchaseOrderLineIds);
}
