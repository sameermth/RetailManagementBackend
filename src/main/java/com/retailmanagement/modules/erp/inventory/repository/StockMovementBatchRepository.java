package com.retailmanagement.modules.erp.inventory.repository;

import com.retailmanagement.modules.erp.inventory.entity.StockMovementBatch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockMovementBatchRepository extends JpaRepository<StockMovementBatch, Long> {}
