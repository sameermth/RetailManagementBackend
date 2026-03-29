package com.retailmanagement.modules.erp.purchase.repository;

import com.retailmanagement.modules.erp.purchase.entity.PurchaseReceiptLineBatch;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseReceiptLineBatchRepository extends JpaRepository<PurchaseReceiptLineBatch, Long> {
    List<PurchaseReceiptLineBatch> findByPurchaseReceiptLineId(Long purchaseReceiptLineId);
}
