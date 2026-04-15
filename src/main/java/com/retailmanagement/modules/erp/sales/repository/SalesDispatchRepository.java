package com.retailmanagement.modules.erp.sales.repository;

import com.retailmanagement.modules.erp.sales.entity.SalesDispatch;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesDispatchRepository extends JpaRepository<SalesDispatch, Long> {

    List<SalesDispatch> findTop100ByOrganizationIdOrderByDispatchDateDescIdDesc(Long organizationId);

    List<SalesDispatch> findBySalesInvoiceIdOrderByDispatchDateDescIdDesc(Long salesInvoiceId);

    Optional<SalesDispatch> findByIdAndOrganizationId(Long id, Long organizationId);
}
