package com.retailmanagement.modules.purchase.repository;

import com.retailmanagement.modules.purchase.model.SupplierPayment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierPaymentRepository extends JpaRepository<SupplierPayment, Long> {

    Optional<SupplierPayment> findByPaymentReference(String paymentReference);

    List<SupplierPayment> findBySupplierId(Long supplierId);

    Page<SupplierPayment> findBySupplierId(Long supplierId, Pageable pageable);

    List<SupplierPayment> findByPurchaseId(Long purchaseId);

    List<SupplierPayment> findByPaymentDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM SupplierPayment p WHERE p.paymentDate BETWEEN :startDate AND :endDate")
    BigDecimal getTotalPaymentsForPeriod(@Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);

    boolean existsByPaymentReference(String paymentReference);

    boolean existsByTransactionId(String transactionId);
}