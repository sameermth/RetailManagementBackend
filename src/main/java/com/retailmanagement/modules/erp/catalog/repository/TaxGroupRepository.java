package com.retailmanagement.modules.erp.catalog.repository;

import com.retailmanagement.modules.erp.catalog.entity.TaxGroup;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxGroupRepository extends JpaRepository<TaxGroup, Long> {
    Optional<TaxGroup> findByIdAndOrganizationId(Long id, Long organizationId);

    List<TaxGroup> findByOrganizationIdAndIsActiveTrueAndCgstRateAndSgstRateAndIgstRateAndCessRate(
            Long organizationId,
            BigDecimal cgstRate,
            BigDecimal sgstRate,
            BigDecimal igstRate,
            BigDecimal cessRate
    );

    List<TaxGroup> findTop30ByOrganizationIdAndIsActiveTrueOrderByNameAsc(Long organizationId);

    List<TaxGroup> findTop30ByOrganizationIdAndIsActiveTrueAndCodeContainingIgnoreCaseOrderByNameAsc(Long organizationId, String code);

    List<TaxGroup> findTop30ByOrganizationIdAndIsActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc(Long organizationId, String name);
}
