package com.retailmanagement.modules.erp.sales.repository;

import com.retailmanagement.modules.erp.sales.entity.SalesQuote;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesQuoteRepository extends JpaRepository<SalesQuote, Long> {
    List<SalesQuote> findTop100ByOrganizationIdOrderByQuoteDateDescIdDesc(Long organizationId);
}
