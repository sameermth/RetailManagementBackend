package com.retailmanagement.modules.purchase.repository;

import com.retailmanagement.modules.purchase.model.PurchaseReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseReceiptRepository extends JpaRepository<PurchaseReceipt, Long> {

    Optional<PurchaseReceipt> findByReceiptNumber(String receiptNumber);

    List<PurchaseReceipt> findByPurchaseId(Long purchaseId);

    boolean existsByReceiptNumber(String receiptNumber);
}