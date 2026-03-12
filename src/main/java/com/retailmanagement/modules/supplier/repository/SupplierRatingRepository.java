package com.retailmanagement.modules.supplier.repository;

import com.retailmanagement.modules.supplier.model.SupplierRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupplierRatingRepository extends JpaRepository<SupplierRating, Long> {

    List<SupplierRating> findBySupplierId(Long supplierId);

    @Query("SELECT AVG(sr.averageRating) FROM SupplierRating sr WHERE sr.supplier.id = :supplierId")
    Double getAverageRatingForSupplier(@Param("supplierId") Long supplierId);

    @Query("SELECT sr FROM SupplierRating sr WHERE sr.purchaseId = :purchaseId")
    List<SupplierRating> findByPurchaseId(@Param("purchaseId") Long purchaseId);
}