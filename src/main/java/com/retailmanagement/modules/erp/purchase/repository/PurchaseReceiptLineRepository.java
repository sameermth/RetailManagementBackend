package com.retailmanagement.modules.erp.purchase.repository;

import com.retailmanagement.modules.erp.purchase.entity.PurchaseReceiptLine;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseReceiptLineRepository extends JpaRepository<PurchaseReceiptLine, Long> {
    List<PurchaseReceiptLine> findByPurchaseReceiptIdOrderByIdAsc(Long purchaseReceiptId);
}
