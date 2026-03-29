package com.retailmanagement.modules.erp.subscription.repository;

import com.retailmanagement.modules.erp.subscription.entity.AccountSubscription;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountSubscriptionRepository extends JpaRepository<AccountSubscription, Long> {

    @Query("""
            SELECT s
            FROM AccountSubscription s
            JOIN FETCH s.plan p
            WHERE s.accountId = :accountId
              AND s.status IN ('ACTIVE', 'TRIALING')
              AND s.startsOn <= :asOfDate
              AND (s.endsOn IS NULL OR s.endsOn >= :asOfDate)
            ORDER BY s.startsOn DESC, s.id DESC
            """)
    List<AccountSubscription> findActiveSubscriptions(
            @Param("accountId") Long accountId,
            @Param("asOfDate") LocalDate asOfDate
    );

    default Optional<AccountSubscription> findCurrentSubscription(Long accountId, LocalDate asOfDate) {
        return findActiveSubscriptions(accountId, asOfDate).stream().findFirst();
    }
}
