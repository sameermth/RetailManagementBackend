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
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

    @Query(value = """
            SELECT
                p.id, p.name, p.sku, c.name, SUM(si.quantity) as quantitySold,
                SUM(si.TOTAL_PRICE) as TOTAL_REVENUE, AVG(si.UNIT_PRICE) as AVERAGE_PRICE
            FROM SALE_ITEMS si 
                JOIN sales s ON si.sale_id = s.id
                JOIN products p ON  si.product_id = p.id
                LEFT JOIN categories c ON p.category_id = c.id
            WHERE
                s.SALE_DATE BETWEEN :startDate AND :endDate
            GROUP BY p.id, p.name, p.sku, c.name
            ORDER BY SUM(si.quantity) DESC""", nativeQuery = true)
    List<Object[]> getTopProductsRaw(
            @Param("startDate") Timestamp startDate,
            @Param("endDate") Timestamp endDate,
            Pageable pageable
    );

    default List<TopProductDTO> getTopProducts(@Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate,
                                               Pageable pageable) {
        Timestamp start = Timestamp.valueOf(startDate);
        Timestamp end = Timestamp.valueOf(endDate);

        return getTopProductsRaw(start, end, pageable).stream()
                .map(row -> TopProductDTO.builder()
                        .productId(((Number) row[0]).longValue())
                        .productName((String) row[1])
                        .sku((String) row[2])
                        .category((String) row[3])
                        .quantitySold(((Number) row[4]).intValue())
                        .totalRevenue((BigDecimal) row[5])
                        .averagePrice((BigDecimal) row[6])
                        .build()
                )
                .collect(Collectors.toList());
    }

    boolean existsByInvoiceNumber(String invoiceNumber);

    @Query(value = """
                        SELECT
                            s.id,
                            'SALE' as type,
                            CONCAT('Sale of ', s.TOTAL_AMOUNT, ' to ', COALESCE(c.name, 'Unknown Customer')),
                            s.INVOICE_NUMBER,
                            u.username,
                            s.SALE_DATE,
                            s.status,
                            s.TOTAL_AMOUNT
                        FROM
                            SALES s LEFT JOIN 
                            CUSTOMERS c on
                            s.customer_id = c.id LEFT JOIN 
                            USERS u on 
                            s.user_id = u.id
                        WHERE
                            s.SALE_DATE >= :localDateTime
                        ORDER BY s.SALE_DATE DESC LIMIT :limit""", nativeQuery = true)
    List<Object[]> getRecentActivitiesRaw(@Param("localDateTime") Timestamp localDateTime, @Param("limit") int limit);

    default List<RecentActivityDTO> getRecentActivities(LocalDateTime localDateTime, int limit) {
        Timestamp timestamp = Timestamp.valueOf(localDateTime);
        return getRecentActivitiesRaw(timestamp, limit).stream()
                .map(row -> RecentActivityDTO.builder()
                        .id(((Number) row[0]).longValue())
                        .type((String) row[1])
                        .description((String) row[2])
                        .reference((String) row[3])
                        .user((String) row[4])
                        .timestamp(((Timestamp) row[5]).toLocalDateTime()) // Convert here
                        .status((String) row[6])
                        .amount((BigDecimal) row[7])
                        .build()
                )
                .collect(Collectors.toList());
    }
}