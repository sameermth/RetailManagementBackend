package com.retailmanagement.modules.customer.repository;

import com.retailmanagement.modules.customer.model.CustomerDue;
import com.retailmanagement.modules.customer.enums.DueStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface CustomerDueRepository extends JpaRepository<CustomerDue, Long> {

    List<CustomerDue> findByCustomerId(Long customerId);

    Page<CustomerDue> findByCustomerId(Long customerId, Pageable pageable);

    List<CustomerDue> findByStatus(DueStatus status);

    @Query("SELECT d FROM CustomerDue d WHERE d.dueDate < :date AND d.status IN ('PENDING', 'PARTIALLY_PAID')")
    List<CustomerDue> findOverdueDues(@Param("date") LocalDate date);

    @Query("SELECT d FROM CustomerDue d WHERE d.dueDate BETWEEN :startDate AND :endDate")
    List<CustomerDue> findDuesInDateRange(@Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(d.remainingAmount), 0) FROM CustomerDue d WHERE d.status IN ('PENDING', 'PARTIALLY_PAID')")
    BigDecimal getTotalDueAmount();

    @Query("SELECT COUNT(DISTINCT d.customer.id) FROM CustomerDue d WHERE d.status IN ('PENDING', 'PARTIALLY_PAID')")
    long countCustomersWithDue();

    @Query("SELECT COALESCE(SUM(d.remainingAmount), 0) FROM CustomerDue d " +
            "WHERE d.dueDate < :date AND d.status IN ('PENDING', 'PARTIALLY_PAID')")
    BigDecimal getTotalOverdueAmount(@Param("date") LocalDate date);

    @Query("SELECT d FROM CustomerDue d WHERE d.reminderCount < 3 AND d.status IN ('PENDING', 'PARTIALLY_PAID')")
    List<CustomerDue> findDuesNeedingReminder();

    @Query("SELECT new map(d.customer.id as customerId, d.customer.name as customerName, " +
            "d.customer.phone as customerPhone, SUM(d.remainingAmount) as totalDue) " +
            "FROM CustomerDue d WHERE d.status IN ('PENDING', 'PARTIALLY_PAID') " +
            "GROUP BY d.customer.id, d.customer.name, d.customer.phone")
    List<Object[]> getCustomerDueSummary();
}