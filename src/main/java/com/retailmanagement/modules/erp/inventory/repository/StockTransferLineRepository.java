package com.retailmanagement.modules.erp.inventory.repository;

import com.retailmanagement.modules.erp.inventory.entity.StockTransferLine;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockTransferLineRepository extends JpaRepository<StockTransferLine, Long> {
    List<StockTransferLine> findByStockTransferId(Long stockTransferId);
}
