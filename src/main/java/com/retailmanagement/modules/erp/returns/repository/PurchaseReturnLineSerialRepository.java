package com.retailmanagement.modules.erp.returns.repository;

import com.retailmanagement.modules.erp.returns.entity.PurchaseReturnLineSerial;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseReturnLineSerialRepository extends JpaRepository<PurchaseReturnLineSerial, Long> {
    List<PurchaseReturnLineSerial> findByPurchaseReturnLineId(Long purchaseReturnLineId);
    List<PurchaseReturnLineSerial> findBySerialNumberId(Long serialNumberId);
}
