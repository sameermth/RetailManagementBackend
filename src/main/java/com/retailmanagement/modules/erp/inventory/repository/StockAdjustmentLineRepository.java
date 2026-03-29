package com.retailmanagement.modules.erp.inventory.repository;

import com.retailmanagement.modules.erp.inventory.entity.StockAdjustmentLine;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockAdjustmentLineRepository extends JpaRepository<StockAdjustmentLine, Long> {
    List<StockAdjustmentLine> findByStockAdjustmentId(Long stockAdjustmentId);
}
