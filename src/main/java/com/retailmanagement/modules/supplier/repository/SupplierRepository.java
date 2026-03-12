package com.retailmanagement.modules.supplier.repository;

import com.retailmanagement.modules.supplier.model.Supplier;
import com.retailmanagement.modules.supplier.enums.SupplierStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    Optional<Supplier> findBySupplierCode(String supplierCode);

    Optional<Supplier> findByEmail(String email);

    Optional<Supplier> findByPhone(String phone);

    Page<Supplier> findByStatus(SupplierStatus status, Pageable pageable);

    @Query("SELECT s FROM Supplier s WHERE " +
            "LOWER(s.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(s.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "s.phone LIKE CONCAT('%', :searchTerm, '%') OR " +
            "LOWER(s.contactPerson) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(s.supplierCode) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Supplier> searchSuppliers(@Param("searchTerm") String searchTerm, Pageable pageable);

    @Query("SELECT s FROM Supplier s WHERE s.outstandingAmount > 0")
    List<Supplier> findSuppliersWithOutstanding();

    @Query("SELECT s FROM Supplier s ORDER BY s.name ASC")
    List<Supplier> findAllOrderedByName();

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    boolean existsBySupplierCode(String supplierCode);
}