package com.retailmanagement.modules.customer.repository;

import com.retailmanagement.modules.customer.model.Customer;
import com.retailmanagement.modules.customer.enums.CustomerStatus;
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
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByCustomerCode(String customerCode);

    Optional<Customer> findByEmail(String email);

    Optional<Customer> findByPhone(String phone);

    Page<Customer> findByStatus(CustomerStatus status, Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE " +
            "LOWER(c.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(c.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "c.phone LIKE CONCAT('%', :searchTerm, '%') OR " +
            "LOWER(c.customerCode) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Customer> searchCustomers(@Param("searchTerm") String searchTerm, Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE c.totalDueAmount > 0")
    List<Customer> findCustomersWithDue();

    @Query("SELECT c FROM Customer c WHERE c.dueReminderEnabled = true")
    List<Customer> findCustomersWithDueReminderEnabled();

    @Query("SELECT COUNT(c) FROM Customer c WHERE DATE(c.createdAt) = :date")
    long countByCreatedDate(@Param("date") LocalDate date);

    @Query("SELECT SUM(c.totalPurchaseAmount) FROM Customer c")
    BigDecimal getTotalPurchaseAmount();

    @Query("SELECT c FROM Customer c ORDER BY c.totalPurchaseAmount DESC")
    List<Customer> findTopCustomers(Pageable pageable);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    boolean existsByCustomerCode(String customerCode);
}