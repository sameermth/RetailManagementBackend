package com.retailmanagement.modules.erp.sales.repository;

import com.retailmanagement.modules.erp.sales.entity.SalesQuoteLine;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesQuoteLineRepository extends JpaRepository<SalesQuoteLine, Long> {
    List<SalesQuoteLine> findBySalesQuoteIdOrderByIdAsc(Long salesQuoteId);
}
