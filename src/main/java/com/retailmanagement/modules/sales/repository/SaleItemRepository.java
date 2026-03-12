package com.retailmanagement.modules.sales.repository;

import com.retailmanagement.modules.sales.model.SaleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {

    List<SaleItem> findBySaleId(Long saleId);

    List<SaleItem> findByProductId(Long productId);

    @Query("SELECT si FROM SaleItem si WHERE si.sale.saleDate BETWEEN :startDate AND :endDate")
    List<SaleItem> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate);

    @Query("SELECT SUM(si.quantity) FROM SaleItem si WHERE si.product.id = :productId " +
            "AND si.sale.saleDate BETWEEN :startDate AND :endDate")
    Integer getTotalQuantitySold(@Param("productId") Long productId,
                                 @Param("startDate") LocalDateTime startDate,
                                 @Param("endDate") LocalDateTime endDate);
}