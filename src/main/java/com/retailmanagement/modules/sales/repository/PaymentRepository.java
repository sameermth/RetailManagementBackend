package com.retailmanagement.modules.sales.repository;

import com.retailmanagement.modules.sales.model.Payment;
import com.retailmanagement.modules.sales.enums.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findBySaleId(Long saleId);

    List<Payment> findByPaymentMethod(PaymentMethod paymentMethod);

    Optional<Payment>  findPaymentByPaymentReference(String paymentReference);

    List<Payment> findByPaymentDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
            "WHERE p.paymentMethod = :method AND p.paymentDate BETWEEN :startDate AND :endDate")
    BigDecimal getTotalByMethodForPeriod(@Param("method") PaymentMethod method,
                                         @Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
            "WHERE p.paymentDate BETWEEN :startDate AND :endDate")
    BigDecimal getTotalPaymentsForPeriod(@Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);

    boolean existsByTransactionId(String transactionId);
}