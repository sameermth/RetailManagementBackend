package com.retailmanagement.modules.erp.returns.repository;

import com.retailmanagement.modules.erp.returns.entity.PurchaseReturnLine;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseReturnLineRepository extends JpaRepository<PurchaseReturnLine, Long> {
    List<PurchaseReturnLine> findByPurchaseReturnIdOrderByIdAsc(Long purchaseReturnId);
    List<PurchaseReturnLine> findByOriginalPurchaseReceiptLineId(Long originalPurchaseReceiptLineId);
}
