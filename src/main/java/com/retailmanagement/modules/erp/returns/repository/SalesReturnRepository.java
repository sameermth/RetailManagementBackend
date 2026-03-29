package com.retailmanagement.modules.erp.returns.repository;

import com.retailmanagement.modules.erp.returns.entity.SalesReturn;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesReturnRepository extends JpaRepository<SalesReturn, Long> {
    List<SalesReturn> findTop100ByOrganizationIdOrderByReturnDateDescIdDesc(Long organizationId);
    List<SalesReturn> findByOrganizationIdAndReturnDateBetweenOrderByReturnDateDescIdDesc(Long organizationId, LocalDate fromDate, LocalDate toDate);
    Optional<SalesReturn> findByOrganizationIdAndId(Long organizationId, Long id);
    List<SalesReturn> findByOrganizationIdAndOriginalSalesInvoiceIdAndStatusAndReturnDateLessThanEqual(Long organizationId, Long originalSalesInvoiceId, String status, LocalDate asOfDate);
}
