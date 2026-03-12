package com.retailmanagement.modules.inventory.repository;

import com.retailmanagement.modules.inventory.enums.MovementType;
import com.retailmanagement.modules.inventory.model.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    List<StockMovement> findByProductIdOrderByMovementDateDesc(Long productId);

    Page<StockMovement> findByProductId(Long productId, Pageable pageable);

    List<StockMovement> findByFromWarehouseIdOrToWarehouseId(Long fromWarehouseId, Long toWarehouseId);

    List<StockMovement> findByMovementType(MovementType movementType);

    @Query("SELECT sm FROM StockMovement sm WHERE sm.movementDate BETWEEN :startDate AND :endDate")
    List<StockMovement> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);

    @Query("SELECT sm FROM StockMovement sm WHERE sm.referenceType = :referenceType AND sm.referenceId = :referenceId")
    List<StockMovement> findByReference(@Param("referenceType") String referenceType,
                                        @Param("referenceId") Long referenceId);

    boolean existsByReferenceNumber(String referenceNumber);
}