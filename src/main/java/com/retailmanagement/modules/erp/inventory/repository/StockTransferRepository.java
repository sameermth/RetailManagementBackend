package com.retailmanagement.modules.erp.inventory.repository;

import com.retailmanagement.modules.erp.inventory.entity.StockTransfer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockTransferRepository extends JpaRepository<StockTransfer, Long> {
    java.util.List<StockTransfer> findTop50ByOrganizationIdOrderByTransferDateDesc(Long organizationId);
}
