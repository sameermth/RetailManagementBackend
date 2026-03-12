package com.retailmanagement.modules.distributor.repository;

import com.retailmanagement.modules.distributor.model.DistributorPayment;
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
public interface DistributorPaymentRepository extends JpaRepository<DistributorPayment, Long> {

    Optional<DistributorPayment> findByPaymentReference(String paymentReference);

    List<DistributorPayment> findByDistributorId(Long distributorId);

    Page<DistributorPayment> findByDistributorId(Long distributorId, Pageable pageable);

    List<DistributorPayment> findByOrderId(Long orderId);

    List<DistributorPayment> findByPaymentDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM DistributorPayment p WHERE p.paymentDate BETWEEN :startDate AND :endDate")
    BigDecimal getTotalPaymentsForPeriod(@Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM DistributorPayment p WHERE p.distributor.id = :distributorId")
    BigDecimal getTotalPaymentsByDistributor(@Param("distributorId") Long distributorId);

    boolean existsByPaymentReference(String paymentReference);

    boolean existsByTransactionId(String transactionId);
}