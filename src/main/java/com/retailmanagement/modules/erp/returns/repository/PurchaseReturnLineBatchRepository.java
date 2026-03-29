package com.retailmanagement.modules.erp.returns.repository;

import com.retailmanagement.modules.erp.returns.entity.PurchaseReturnLineBatch;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseReturnLineBatchRepository extends JpaRepository<PurchaseReturnLineBatch, Long> {
    List<PurchaseReturnLineBatch> findByPurchaseReturnLineId(Long purchaseReturnLineId);
}
