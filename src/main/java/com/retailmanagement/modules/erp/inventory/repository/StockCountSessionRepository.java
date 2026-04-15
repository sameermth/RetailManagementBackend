package com.retailmanagement.modules.erp.inventory.repository;

import com.retailmanagement.modules.erp.inventory.entity.StockCountSession;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockCountSessionRepository extends JpaRepository<StockCountSession, Long> {
    List<StockCountSession> findByOrganizationIdOrderByCountDateDescIdDesc(Long organizationId);
    List<StockCountSession> findByOrganizationIdAndWarehouseIdOrderByCountDateDescIdDesc(Long organizationId, Long warehouseId);
}
