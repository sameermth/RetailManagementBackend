package com.retailmanagement.modules.erp.sales.repository;

import com.retailmanagement.modules.erp.sales.entity.CustomerReceiptAllocation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerReceiptAllocationRepository extends JpaRepository<CustomerReceiptAllocation, Long> {
    List<CustomerReceiptAllocation> findByCustomerReceiptIdOrderByIdAsc(Long customerReceiptId);
    List<CustomerReceiptAllocation> findBySalesInvoiceIdOrderByIdAsc(Long salesInvoiceId);
}
