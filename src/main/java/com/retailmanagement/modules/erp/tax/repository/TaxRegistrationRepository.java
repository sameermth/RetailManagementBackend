package com.retailmanagement.modules.erp.tax.repository;

import com.retailmanagement.modules.erp.tax.entity.TaxRegistration;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaxRegistrationRepository extends JpaRepository<TaxRegistration, Long> {

    @Query("""
            SELECT tr
            FROM TaxRegistration tr
            WHERE tr.organizationId = :organizationId
              AND ((:branchId IS NOT NULL AND tr.branchId = :branchId) OR tr.branchId IS NULL)
              AND tr.isActive = true
              AND tr.effectiveFrom <= :documentDate
              AND (tr.effectiveTo IS NULL OR tr.effectiveTo >= :documentDate)
            ORDER BY CASE WHEN tr.branchId = :branchId THEN 0 ELSE 1 END,
                     CASE WHEN tr.isDefault = true THEN 0 ELSE 1 END,
                     tr.effectiveFrom DESC
            """)
    List<TaxRegistration> findApplicableRegistrations(
            @Param("organizationId") Long organizationId,
            @Param("branchId") Long branchId,
            @Param("documentDate") LocalDate documentDate
    );

    default Optional<TaxRegistration> findApplicableRegistration(Long organizationId, Long branchId, LocalDate documentDate) {
        return findApplicableRegistrations(organizationId, branchId, documentDate).stream().findFirst();
    }
}
