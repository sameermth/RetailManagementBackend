package com.retailmanagement.modules.erp.inventory.repository;

import com.retailmanagement.modules.erp.inventory.entity.StockAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockAdjustmentRepository extends JpaRepository<StockAdjustment, Long> {
    java.util.List<StockAdjustment> findTop50ByOrganizationIdOrderByAdjustmentDateDesc(Long organizationId);
}
