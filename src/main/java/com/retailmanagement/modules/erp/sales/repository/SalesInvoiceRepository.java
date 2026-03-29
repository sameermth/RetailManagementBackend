package com.retailmanagement.modules.erp.sales.repository;

import com.retailmanagement.modules.erp.sales.entity.SalesInvoice;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesInvoiceRepository extends JpaRepository<SalesInvoice, Long> {

    List<SalesInvoice> findTop100ByOrganizationIdOrderByInvoiceDateDescIdDesc(Long organizationId);

    List<SalesInvoice> findByOrganizationIdOrderByIdDesc(Long organizationId);

    List<SalesInvoice> findByOrganizationIdOrderByDueDateAscIdAsc(Long organizationId);

    Optional<SalesInvoice> findByIdAndOrganizationId(Long id, Long organizationId);

    Optional<SalesInvoice> findByOrganizationIdAndId(Long organizationId, Long id);
}
