package com.retailmanagement.modules.distributor.repository;

import com.retailmanagement.modules.distributor.model.DistributorOrder;
import com.retailmanagement.modules.distributor.enums.DistributorOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DistributorOrderRepository extends JpaRepository<DistributorOrder, Long> {

    Optional<DistributorOrder> findByOrderNumber(String orderNumber);

    List<DistributorOrder> findByDistributorId(Long distributorId);

    Page<DistributorOrder> findByDistributorId(Long distributorId, Pageable pageable);

    List<DistributorOrder> findByStatus(DistributorOrderStatus status);

    Page<DistributorOrder> findByStatus(DistributorOrderStatus status, Pageable pageable);

    @Query("SELECT o FROM DistributorOrder o WHERE o.orderDate BETWEEN :startDate AND :endDate")
    List<DistributorOrder> findByOrderDateBetween(@Param("startDate") LocalDateTime startDate,
                                                  @Param("endDate") LocalDateTime endDate);

    @Query("SELECT o FROM DistributorOrder o WHERE o.expectedDeliveryDate < :date AND o.status IN ('APPROVED', 'PROCESSING')")
    List<DistributorOrder> findDelayedOrders(@Param("date") LocalDateTime date);

    @Query("SELECT SUM(o.totalAmount) FROM DistributorOrder o WHERE o.orderDate BETWEEN :startDate AND :endDate")
    Double getTotalOrderAmountForPeriod(@Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);

    boolean existsByOrderNumber(String orderNumber);
}