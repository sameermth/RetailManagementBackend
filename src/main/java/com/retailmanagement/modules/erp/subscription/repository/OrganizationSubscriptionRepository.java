package com.retailmanagement.modules.erp.subscription.repository;

import com.retailmanagement.modules.erp.subscription.entity.OrganizationSubscription;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrganizationSubscriptionRepository extends JpaRepository<OrganizationSubscription, Long> {

    @Query("""
            SELECT os
            FROM OrganizationSubscription os
            JOIN FETCH os.plan p
            WHERE os.organizationId = :organizationId
              AND os.status IN ('ACTIVE', 'TRIALING')
              AND os.startsOn <= :asOfDate
              AND (os.endsOn IS NULL OR os.endsOn >= :asOfDate)
            ORDER BY os.startsOn DESC, os.id DESC
            """)
    java.util.List<OrganizationSubscription> findActiveSubscriptions(
            @Param("organizationId") Long organizationId,
            @Param("asOfDate") LocalDate asOfDate
    );

    default Optional<OrganizationSubscription> findCurrentSubscription(Long organizationId, LocalDate asOfDate) {
        return findActiveSubscriptions(organizationId, asOfDate).stream().findFirst();
    }
}
