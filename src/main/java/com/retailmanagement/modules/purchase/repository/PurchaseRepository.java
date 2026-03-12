package com.retailmanagement.modules.purchase.repository;

import com.retailmanagement.modules.purchase.model.Purchase;
import com.retailmanagement.modules.purchase.enums.PurchaseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    Optional<Purchase> findByPurchaseOrderNumber(String purchaseOrderNumber);

    List<Purchase> findBySupplierId(Long supplierId);

    Page<Purchase> findBySupplierId(Long supplierId, Pageable pageable);

    List<Purchase> findByStatus(PurchaseStatus status);

    Page<Purchase> findByStatus(PurchaseStatus status, Pageable pageable);

    @Query("SELECT p FROM Purchase p WHERE p.orderDate BETWEEN :startDate AND :endDate")
    List<Purchase> findByOrderDateBetween(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    @Query("SELECT p FROM Purchase p WHERE p.expectedDeliveryDate < :date AND p.status IN ('ORDERED', 'APPROVED')")
    List<Purchase> findDelayedPurchases(@Param("date") LocalDateTime date);

    @Query("SELECT SUM(p.totalAmount) FROM Purchase p WHERE p.orderDate BETWEEN :startDate AND :endDate")
    Double getTotalPurchaseAmountForPeriod(@Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(p) FROM Purchase p WHERE p.status = 'PENDING_APPROVAL'")
    Long countPendingApproval();

    boolean existsByPurchaseOrderNumber(String purchaseOrderNumber);
}