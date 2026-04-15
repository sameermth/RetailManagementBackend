package com.retailmanagement.modules.erp.sales.repository;

import com.retailmanagement.modules.erp.sales.entity.SalesInvoicePaymentRequest;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesInvoicePaymentRequestRepository extends JpaRepository<SalesInvoicePaymentRequest, Long> {
    List<SalesInvoicePaymentRequest> findTop100ByOrganizationIdOrderByRequestDateDescIdDesc(Long organizationId);
    List<SalesInvoicePaymentRequest> findBySalesInvoiceIdOrderByRequestDateDescIdDesc(Long salesInvoiceId);
    Optional<SalesInvoicePaymentRequest> findByIdAndOrganizationId(Long id, Long organizationId);
}
