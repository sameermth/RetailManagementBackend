package com.retailmanagement.modules.purchase.repository;

import com.retailmanagement.modules.purchase.model.PurchaseItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseItemRepository extends JpaRepository<PurchaseItem, Long> {

    List<PurchaseItem> findByPurchaseId(Long purchaseId);

    List<PurchaseItem> findByProductId(Long productId);

    @Query("SELECT pi FROM PurchaseItem pi WHERE pi.product.id = :productId AND pi.receivedQuantity < pi.quantity")
    List<PurchaseItem> findPendingItemsByProduct(@Param("productId") Long productId);
}