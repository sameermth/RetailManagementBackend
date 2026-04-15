package com.retailmanagement.modules.erp.inventory.repository;

import com.retailmanagement.modules.erp.inventory.entity.StockCountLine;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockCountLineRepository extends JpaRepository<StockCountLine, Long> {
    List<StockCountLine> findByStockCountSessionIdOrderByIdAsc(Long stockCountSessionId);
    Optional<StockCountLine> findByStockCountSessionIdAndProductIdAndBatchId(Long stockCountSessionId, Long productId, Long batchId);
}
