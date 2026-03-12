package com.retailmanagement.modules.sales.repository;

import com.retailmanagement.modules.sales.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    Optional<Invoice> findBySaleId(Long saleId);

    List<Invoice> findByInvoiceDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    List<Invoice> findByCustomerEmail(String email);

    boolean existsByInvoiceNumber(String invoiceNumber);
}