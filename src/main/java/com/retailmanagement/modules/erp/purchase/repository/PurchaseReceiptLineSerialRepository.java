package com.retailmanagement.modules.erp.purchase.repository;

import com.retailmanagement.modules.erp.purchase.entity.PurchaseReceiptLineSerial;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseReceiptLineSerialRepository extends JpaRepository<PurchaseReceiptLineSerial, Long> {
    List<PurchaseReceiptLineSerial> findByPurchaseReceiptLineId(Long purchaseReceiptLineId);
}
