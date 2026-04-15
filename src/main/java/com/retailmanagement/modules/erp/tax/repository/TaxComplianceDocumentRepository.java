package com.retailmanagement.modules.erp.tax.repository;

import com.retailmanagement.modules.erp.tax.entity.TaxComplianceDocument;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxComplianceDocumentRepository extends JpaRepository<TaxComplianceDocument, Long> {

    List<TaxComplianceDocument> findByOrganizationIdAndSourceTypeAndSourceIdOrderByIdDesc(
            Long organizationId,
            String sourceType,
            Long sourceId
    );

    Optional<TaxComplianceDocument> findByIdAndOrganizationId(Long id, Long organizationId);
}
