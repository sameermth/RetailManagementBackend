package com.retailmanagement.modules.erp.purchase.repository;

import com.retailmanagement.modules.erp.purchase.entity.SupplierPaymentAllocation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierPaymentAllocationRepository extends JpaRepository<SupplierPaymentAllocation, Long> {
    List<SupplierPaymentAllocation> findByPurchaseReceiptId(Long purchaseReceiptId);
    List<SupplierPaymentAllocation> findBySupplierPaymentIdOrderByIdAsc(Long supplierPaymentId);
}
