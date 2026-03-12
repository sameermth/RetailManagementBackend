package com.retailmanagement.modules.customer.repository;

import com.retailmanagement.modules.customer.model.LoyaltyTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoyaltyTransactionRepository extends JpaRepository<LoyaltyTransaction, Long> {

    List<LoyaltyTransaction> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    @Query("SELECT COALESCE(SUM(l.points), 0) FROM LoyaltyTransaction l " +
            "WHERE l.customer.id = :customerId AND l.transactionType = 'EARNED' AND l.isExpired = false")
    Integer getTotalEarnedPoints(@Param("customerId") Long customerId);

    @Query("SELECT COALESCE(SUM(l.points), 0) FROM LoyaltyTransaction l " +
            "WHERE l.customer.id = :customerId AND l.transactionType = 'REDEEMED'")
    Integer getTotalRedeemedPoints(@Param("customerId") Long customerId);

    @Query("SELECT l FROM LoyaltyTransaction l WHERE l.isExpired = false AND l.expiryDate < CURRENT_DATE")
    List<LoyaltyTransaction> findExpiredTransactions();
}