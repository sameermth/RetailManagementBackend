package com.retailmanagement.modules.sales.repository;

import com.retailmanagement.modules.dashboard.dto.RecentActivityDTO;
import com.retailmanagement.modules.dashboard.dto.TopProductDTO;
import com.retailmanagement.modules.sales.model.Sale;
import com.retailmanagement.modules.sales.enums.SaleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {

    List<Sale> findByCustomerId(Long customerId);

    Page<Sale> findByCustomerId(Long customerId, Pageable pageable);

    List<Sale> findByUserId(Long userId);

    List<Sale> findByStatus(SaleStatus status);

    Optional<Sale> findByInvoiceNumber(String invoiceNumber);

    List<Sale> findBySaleDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT s FROM Sale s WHERE DATE(s.saleDate) = :date")
    List<Sale> findBySaleDate(@Param("date") LocalDate date);

    @Query("SELECT s FROM Sale s WHERE s.dueDate < :date AND s.pendingAmount > 0")
    List<Sale> findOverdueSales(@Param("date") LocalDateTime date);

    @Query("SELECT SUM(s.totalAmount) FROM Sale s WHERE s.saleDate BETWEEN :startDate AND :endDate")
    Double getTotalSalesForPeriod(@Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(s) FROM Sale s WHERE s.saleDate BETWEEN :startDate AND :endDate")
    Long countSalesForPeriod(@Param("startDate") LocalDateTime startDate,
                             @Param("endDate") LocalDateTime endDate);

    @Query("SELECT AVG(s.totalAmount) FROM Sale s WHERE s.saleDate BETWEEN :startDate AND :endDate")
    BigDecimal getAverageTransactionValue(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(s) FROM Sale s WHERE s.status = 'PENDING'")
    Long countPendingOrders();

    @Query("SELECT COUNT(s) FROM Sale s WHERE DATE(s.saleDate) = :date AND s.status = 'COMPLETED'")
    Long countCompletedOrders(@Param("date") LocalDate date);

    @Query("SELECT new com.retailmanagement.modules.sales.dto.TopProductDTO(" +
            "p.id, p.name, p.sku, c.name, SUM(si.quantity), SUM(si.totalPrice), AVG(si.unitPrice)) " +
            "FROM SaleItem si JOIN si.product p LEFT JOIN p.category c " +
            "WHERE si.sale.saleDate BETWEEN :startDate AND :endDate " +
            "GROUP BY p.id, p.name, p.sku, c.name " +
            "ORDER BY SUM(si.quantity) DESC")
    List<TopProductDTO> getTopProducts(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate,
                                       Pageable pageable);

    boolean existsByInvoiceNumber(String invoiceNumber);

    List<RecentActivityDTO> getRecentActivities(LocalDateTime localDateTime, int limit);
}